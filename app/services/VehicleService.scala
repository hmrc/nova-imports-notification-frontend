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
import models.{ImportNumber, SupplierNumber, UserAnswers, VehicleNumber}
import play.api.libs.json.{JsObject, Json}
import queries.AllVehiclesQuery
import repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@ImplementedBy(classOf[VehicleServiceImpl])
trait VehicleService {

  def addForSupplier(answers: UserAnswers, supplierNumber: SupplierNumber): Future[VehicleNumber]

  def addForImport(answers: UserAnswers, importNumber: ImportNumber): Future[VehicleNumber]

  def numberExists(answers: UserAnswers, vehicleNumber: VehicleNumber): Boolean

  def belongsToSupplier(answers: UserAnswers, vehicleNumber: VehicleNumber, supplierNumber: SupplierNumber): Boolean

  def delete(answers: UserAnswers, vehicleNumber: VehicleNumber): Future[UserAnswers]

  // these removeFors don't save. delete calls them to remove a supplier or import with its vehicles in one write
  def removeForSupplier(answers: UserAnswers, supplierNumber: SupplierNumber): Try[UserAnswers]

  def removeForImport(answers: UserAnswers, importNumber: ImportNumber): Try[UserAnswers]

  def count(answers: UserAnswers): Int

  def inOrder(answers: UserAnswers): Seq[(VehicleNumber, JsObject)]
}

@Singleton
class VehicleServiceImpl @Inject() (
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends VehicleService {

  def addForSupplier(answers: UserAnswers, supplierNumber: SupplierNumber): Future[VehicleNumber] =
    add(answers, Json.obj("supplierNumber" -> supplierNumber.value))

  def addForImport(answers: UserAnswers, importNumber: ImportNumber): Future[VehicleNumber] =
    add(answers, Json.obj("importNumber" -> importNumber.value))

  def numberExists(answers: UserAnswers, vehicleNumber: VehicleNumber): Boolean =
    allVehicles(answers).contains(vehicleNumber.value.toString)

  def belongsToSupplier(answers: UserAnswers, vehicleNumber: VehicleNumber, supplierNumber: SupplierNumber): Boolean =
    allVehicles(answers)
      .get(vehicleNumber.value.toString)
      .exists(vehicle => (vehicle \ "supplierNumber").asOpt[Int].contains(supplierNumber.value))

  def delete(answers: UserAnswers, vehicleNumber: VehicleNumber): Future[UserAnswers] =
    save(answers, allVehicles(answers) - vehicleNumber.value.toString)

  def removeForSupplier(answers: UserAnswers, supplierNumber: SupplierNumber): Try[UserAnswers] =
    removeVehiclesFor(answers, "supplierNumber", supplierNumber.value)

  def removeForImport(answers: UserAnswers, importNumber: ImportNumber): Try[UserAnswers] =
    removeVehiclesFor(answers, "importNumber", importNumber.value)

  def count(answers: UserAnswers): Int =
    allVehicles(answers).size

  def inOrder(answers: UserAnswers): Seq[(VehicleNumber, JsObject)] =
    allVehicles(answers).toSeq
      .flatMap { case (key, vehicle) => key.toIntOption.map(number => VehicleNumber(number) -> vehicle) }
      .sortBy { case (number, _) => number.value }

  private def removeVehiclesFor(answers: UserAnswers, ownerField: String, ownerNumber: Int): Try[UserAnswers] =
    answers.set(
      AllVehiclesQuery,
      allVehicles(answers).filterNot { case (_, vehicle) => (vehicle \ ownerField).asOpt[Int].contains(ownerNumber) }
    )

  // the vehicle record is created holding the supplier or import number it belongs to
  private def add(answers: UserAnswers, belongsTo: JsObject): Future[VehicleNumber] = {
    val vehicles = allVehicles(answers)
    val number   = VehicleNumber(vehicles.keys.flatMap(_.toIntOption).maxOption.getOrElse(0) + 1)
    val updated  = vehicles + (number.value.toString -> belongsTo)

    save(answers, updated).map(_ => number)
  }

  private def save(answers: UserAnswers, vehicles: Map[String, JsObject]): Future[UserAnswers] =
    sessionRepository.setPage(answers, AllVehiclesQuery, vehicles)

  private def allVehicles(answers: UserAnswers): Map[String, JsObject] =
    answers.get(AllVehiclesQuery).getOrElse(Map.empty)
}
