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
import connectors.NovaImportsBackendConnector
import models.{BusinessOrPrivateIndividual, SupplierNumber, UserAnswers, UserContext}
import pages.sections.initialquestions.BusinessOrPrivatePage
import pages.sections.notifierdetails.{BusinessNamePage, NameDetailsPage}
import pages.sections.supplierdetails.{SupplierBusinessNamePage, SupplierBusinessOrIndividualPage, SupplierNamePage, UsePersonalDetailsAsSupplierPage, UsePurchaserDetailsAsSupplierPage}
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import queries.AllSuppliersQuery
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[SupplierServiceImpl])
trait SupplierService {

  def add(answers: UserAnswers): Future[SupplierNumber]

  // checks the numbered collection exists but may be empty, so used on the first question only
  def numberExists(answers: UserAnswers, supplierNumber: SupplierNumber): Boolean

  def numberHasValues(answers: UserAnswers, supplierNumber: SupplierNumber): Boolean

  def deleteValues(answers: UserAnswers, supplierNumber: SupplierNumber): Future[UserAnswers]

  def inOrder(answers: UserAnswers): Seq[(SupplierNumber, JsObject)]

  def supplierName(answers: UserAnswers, userContext: UserContext, supplierNumber: SupplierNumber)(implicit
    hc: HeaderCarrier
  ): Future[Option[String]]
}

@Singleton
class SupplierServiceImpl @Inject() (
  sessionRepository: SessionRepository,
  vehicleService: VehicleService,
  connector: NovaImportsBackendConnector
)(implicit ec: ExecutionContext)
    extends SupplierService
    with Logging {

  import SupplierServiceImpl.*

  // an empty numbered collection is used when available till first question is answered
  def add(answers: UserAnswers): Future[SupplierNumber] = {
    val suppliers = allSuppliers(answers)
    val number    = availableNumber(suppliers).getOrElse(nextNumber(suppliers))

    val updated = suppliers + (number.value.toString -> Json.obj())

    sessionRepository.setPage(answers, AllSuppliersQuery, updated).map(_ => number)
  }

  def numberExists(answers: UserAnswers, supplierNumber: SupplierNumber): Boolean =
    allSuppliers(answers).contains(supplierNumber.value.toString)

  def numberHasValues(answers: UserAnswers, supplierNumber: SupplierNumber): Boolean =
    suppliersWithValues(answers).contains(supplierNumber.value.toString)

  // values cleared, collection not removed, so the number never comes back per notification
  // emptying a supplier empties its vehicles too
  def deleteValues(answers: UserAnswers, supplierNumber: SupplierNumber): Future[UserAnswers] = {
    val suppliers = allSuppliers(answers)
    val key       = supplierNumber.value.toString

    // clears the answers, keeps the numbered key. Skips it if the number is not there
    val emptied = if (suppliers.contains(key)) suppliers + (key -> DeletedSupplier) else suppliers

    val updated = for {
      vehiclesEmptied <- vehicleService.deleteValuesForSupplier(answers, supplierNumber)
      supplierEmptied <- vehiclesEmptied.set(AllSuppliersQuery, emptied)
    } yield supplierEmptied

    Future.fromTry(updated).flatMap(saved => sessionRepository.set(saved).map(_ => saved))
  }

  def inOrder(answers: UserAnswers): Seq[(SupplierNumber, JsObject)] =
    suppliersWithValues(answers).toSeq
      .flatMap { case (key, supplier) => key.toIntOption.map(number => SupplierNumber(number) -> supplier) }
      .sortBy { case (number, _) => number.value }

  def supplierName(answers: UserAnswers, userContext: UserContext, supplierNumber: SupplierNumber)(implicit
    hc: HeaderCarrier
  ): Future[Option[String]] =
    (
      answers.get(UsePersonalDetailsAsSupplierPage(supplierNumber)),
      answers.get(UsePurchaserDetailsAsSupplierPage(supplierNumber))
    ) match {
      case (Some(true), _) if userContext.usesTraderDetails => traderName
      case (Some(true), _)                                  => Future.successful(notifierName(answers, userContext))
      case (_, Some(true))                                  => Future.successful(answers.purchaserName)
      case _                                                => Future.successful(enteredSupplierName(answers, supplierNumber))
    }

  private def notifierName(answers: UserAnswers, userContext: UserContext): Option[String] =
    if (userContext.isVatRegisteredOrganisation)
      answers.get(NameDetailsPage).map(_.displayName)
    else
      answers.get(BusinessOrPrivatePage) match {
        case Some(BusinessOrPrivateIndividual.Business)          => answers.get(BusinessNamePage)
        case Some(BusinessOrPrivateIndividual.PrivateIndividual) => answers.get(NameDetailsPage).map(_.displayName)
        case None                                                => None
      }

  private def enteredSupplierName(answers: UserAnswers, supplierNumber: SupplierNumber): Option[String] =
    answers.get(SupplierBusinessOrIndividualPage(supplierNumber)) match {
      case Some(BusinessOrPrivateIndividual.Business)          => answers.get(SupplierBusinessNamePage(supplierNumber))
      case Some(BusinessOrPrivateIndividual.PrivateIndividual) =>
        answers.get(SupplierNamePage(supplierNumber)).map(_.displayName)
      case None => None
    }

  private def traderName(implicit hc: HeaderCarrier): Future[Option[String]] =
    connector
      .getTraderInformation()
      .map {
        case Right(traderInformation) => traderInformation.name
        case Left(error)              =>
          logger.warn(s"Failed to fetch trader information for the supplier name: $error")
          None
      }
      .recover { case NonFatal(e) =>
        logger.warn("Failed to fetch trader information for the supplier name", e)
        None
      }

  // every supplier, empty and deleted included
  private def allSuppliers(answers: UserAnswers): Map[String, JsObject] =
    answers.get(AllSuppliersQuery).getOrElse(Map.empty)

  private def suppliersWithValues(answers: UserAnswers): Map[String, JsObject] =
    allSuppliers(answers).filter { case (_, supplier) => hasValues(supplier) }

  private def hasValues(supplier: JsObject): Boolean =
    supplier.keys.exists(key => !ReservedKeys.contains(key))

  private def isDeleted(supplier: JsObject): Boolean =
    (supplier \ DeletedKey).asOpt[Boolean].contains(true)

  private def availableNumber(suppliers: Map[String, JsObject]): Option[SupplierNumber] =
    suppliers.toSeq
      .flatMap { case (key, supplier) => key.toIntOption.filter(_ => !hasValues(supplier) && !isDeleted(supplier)) }
      .minOption
      .map(SupplierNumber(_))

  private def nextNumber(suppliers: Map[String, JsObject]): SupplierNumber =
    SupplierNumber(suppliers.keys.flatMap(_.toIntOption).maxOption.getOrElse(0) + 1)
}

object SupplierServiceImpl {

  private[services] val DeletedKey = "deleted"

  private[services] val DeletedSupplier: JsObject = Json.obj(DeletedKey -> true)

  // reserved keys are ignored when checking for values
  private[services] val ReservedKeys: Set[String] = Set(DeletedKey)
}
