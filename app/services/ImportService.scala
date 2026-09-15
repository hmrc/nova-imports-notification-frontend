/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import com.google.inject.{ImplementedBy, Inject, Singleton}
import models.{ImportNumber, UserAnswers}
import play.api.libs.json.{JsObject, Json}
import queries.AllImportsQuery
import repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ImportServiceImpl])
trait ImportService {

  def add(answers: UserAnswers): Future[ImportNumber]

  // checks the numbered collection exists but may be empty, so used on the first question only
  def numberExists(answers: UserAnswers, importNumber: ImportNumber): Boolean

  def numberHasValues(answers: UserAnswers, importNumber: ImportNumber): Boolean

  def deleteValues(answers: UserAnswers, importNumber: ImportNumber): Future[UserAnswers]

  def inOrder(answers: UserAnswers): Seq[(ImportNumber, JsObject)]
}

@Singleton
class ImportServiceImpl @Inject() (
  sessionRepository: SessionRepository,
  vehicleService: VehicleService
)(implicit ec: ExecutionContext)
    extends ImportService {

  import ImportServiceImpl.*

  // an empty numbered collection is used when available till first question is answered
  def add(answers: UserAnswers): Future[ImportNumber] = {
    val imports = allImports(answers)
    val number  = availableNumber(imports).getOrElse(nextNumber(imports))

    val updated = imports + (number.value.toString -> Json.obj())

    sessionRepository.setPage(answers, AllImportsQuery, updated).map(_ => number)
  }

  def numberExists(answers: UserAnswers, importNumber: ImportNumber): Boolean =
    allImports(answers).contains(importNumber.value.toString)

  def numberHasValues(answers: UserAnswers, importNumber: ImportNumber): Boolean =
    importsWithValues(answers).contains(importNumber.value.toString)

  // values cleared, collection not removed, so the number never comes back per notification
  // emptying an import empties its vehicles too
  def deleteValues(answers: UserAnswers, importNumber: ImportNumber): Future[UserAnswers] = {
    val imports = allImports(answers)
    val key     = importNumber.value.toString

    // clears the answers, keeps the numbered key. Skips it if the number is not there
    val emptied = if (imports.contains(key)) imports + (key -> DeletedImport) else imports

    val updated = for {
      vehiclesEmptied <- vehicleService.deleteValuesForImport(answers, importNumber)
      importEmptied   <- vehiclesEmptied.set(AllImportsQuery, emptied)
    } yield importEmptied

    Future.fromTry(updated).flatMap(saved => sessionRepository.set(saved).map(_ => saved))
  }

  def inOrder(answers: UserAnswers): Seq[(ImportNumber, JsObject)] =
    importsWithValues(answers).toSeq
      .flatMap { case (key, anImport) => key.toIntOption.map(number => ImportNumber(number) -> anImport) }
      .sortBy { case (number, _) => number.value }

  // every import, empty and deleted included
  private def allImports(answers: UserAnswers): Map[String, JsObject] =
    answers.get(AllImportsQuery).getOrElse(Map.empty)

  private def importsWithValues(answers: UserAnswers): Map[String, JsObject] =
    allImports(answers).filter { case (_, anImport) => hasValues(anImport) }

  private def hasValues(anImport: JsObject): Boolean =
    anImport.keys.exists(key => !ReservedKeys.contains(key))

  private def isDeleted(anImport: JsObject): Boolean =
    (anImport \ DeletedKey).asOpt[Boolean].contains(true)

  private def availableNumber(imports: Map[String, JsObject]): Option[ImportNumber] =
    imports.toSeq
      .flatMap { case (key, anImport) => key.toIntOption.filter(_ => !hasValues(anImport) && !isDeleted(anImport)) }
      .minOption
      .map(ImportNumber(_))

  private def nextNumber(imports: Map[String, JsObject]): ImportNumber =
    ImportNumber(imports.keys.flatMap(_.toIntOption).maxOption.getOrElse(0) + 1)
}

object ImportServiceImpl {

  private[services] val DeletedKey = "deleted"

  private[services] val DeletedImport: JsObject = Json.obj(DeletedKey -> true)

  // reserved keys are ignored when checking for values
  private[services] val ReservedKeys: Set[String] = Set(DeletedKey)
}
