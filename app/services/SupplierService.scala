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
import models.{SupplierNumber, UserAnswers}
import play.api.libs.json.{JsObject, Json}
import queries.AllSuppliersQuery
import repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SupplierServiceImpl])
trait SupplierService {

  def add(answers: UserAnswers): Future[SupplierNumber]

  def numberExists(answers: UserAnswers, supplierNumber: SupplierNumber): Boolean

  def delete(answers: UserAnswers, supplierNumber: SupplierNumber): Future[UserAnswers]

  def inOrder(answers: UserAnswers): Seq[(SupplierNumber, JsObject)]
}

@Singleton
class SupplierServiceImpl @Inject() (
  sessionRepository: SessionRepository,
  vehicleService: VehicleService
)(implicit ec: ExecutionContext)
    extends SupplierService {

  def add(answers: UserAnswers): Future[SupplierNumber] = {
    val suppliers = allSuppliers(answers)
    val number    = SupplierNumber(suppliers.keys.flatMap(_.toIntOption).maxOption.getOrElse(0) + 1)

    val updated = suppliers + (number.value.toString -> Json.obj())

    sessionRepository.setPage(answers, AllSuppliersQuery, updated).map(_ => number)
  }

  def numberExists(answers: UserAnswers, supplierNumber: SupplierNumber): Boolean =
    allSuppliers(answers).contains(supplierNumber.value.toString)

  // deleting a supplier deletes its associated vehicles with it
  def delete(answers: UserAnswers, supplierNumber: SupplierNumber): Future[UserAnswers] = {
    val remaining = allSuppliers(answers) - supplierNumber.value.toString

    val updated = for {
      withoutVehicles <- vehicleService.removeForSupplier(answers, supplierNumber)
      withoutSupplier <- withoutVehicles.set(AllSuppliersQuery, remaining)
    } yield withoutSupplier

    Future.fromTry(updated).flatMap(saved => sessionRepository.set(saved).map(_ => saved))
  }

  def inOrder(answers: UserAnswers): Seq[(SupplierNumber, JsObject)] =
    allSuppliers(answers).toSeq
      .flatMap { case (key, supplier) => key.toIntOption.map(number => SupplierNumber(number) -> supplier) }
      .sortBy { case (number, _) => number.value }

  private def allSuppliers(answers: UserAnswers): Map[String, JsObject] =
    answers.get(AllSuppliersQuery).getOrElse(Map.empty)
}
