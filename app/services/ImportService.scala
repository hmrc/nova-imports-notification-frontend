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

  def numberExists(answers: UserAnswers, importNumber: ImportNumber): Boolean

  def delete(answers: UserAnswers, importNumber: ImportNumber): Future[UserAnswers]

  def inOrder(answers: UserAnswers): Seq[(ImportNumber, JsObject)]
}

@Singleton
class ImportServiceImpl @Inject() (
  sessionRepository: SessionRepository,
  vehicleService: VehicleService
)(implicit ec: ExecutionContext)
    extends ImportService {

  def add(answers: UserAnswers): Future[ImportNumber] = {
    val imports = allImports(answers)
    val number  = ImportNumber(imports.keys.flatMap(_.toIntOption).maxOption.getOrElse(0) + 1)

    val updated = imports + (number.value.toString -> Json.obj())

    sessionRepository.setPage(answers, AllImportsQuery, updated).map(_ => number)
  }

  def numberExists(answers: UserAnswers, importNumber: ImportNumber): Boolean =
    allImports(answers).contains(importNumber.value.toString)

  // deleting an import deletes its associated vehicles with it
  def delete(answers: UserAnswers, importNumber: ImportNumber): Future[UserAnswers] = {
    val remaining = allImports(answers) - importNumber.value.toString

    val updated = for {
      withoutVehicles <- vehicleService.removeForImport(answers, importNumber)
      withoutImport   <- withoutVehicles.set(AllImportsQuery, remaining)
    } yield withoutImport

    Future.fromTry(updated).flatMap(saved => sessionRepository.set(saved).map(_ => saved))
  }

  def inOrder(answers: UserAnswers): Seq[(ImportNumber, JsObject)] =
    allImports(answers).toSeq
      .flatMap { case (key, anImport) => key.toIntOption.map(number => ImportNumber(number) -> anImport) }
      .sortBy { case (number, _) => number.value }

  private def allImports(answers: UserAnswers): Map[String, JsObject] =
    answers.get(AllImportsQuery).getOrElse(Map.empty)
}
