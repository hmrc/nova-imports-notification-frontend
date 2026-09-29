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
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import queries.AllVehiclesQuery
import repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@ImplementedBy(classOf[VehicleServiceImpl])
trait VehicleService {

  def addForSupplier(answers: UserAnswers, supplierNumber: SupplierNumber): Future[VehicleNumber]

  def addForImport(answers: UserAnswers, importNumber: ImportNumber): Future[VehicleNumber]

  // checks the numbered collection exists and is not deleted, may be empty, so used on the first question only
  def numberExists(answers: UserAnswers, vehicleNumber: VehicleNumber): Boolean

  def numberHasValues(answers: UserAnswers, vehicleNumber: VehicleNumber): Boolean

  def belongsToSupplier(answers: UserAnswers, vehicleNumber: VehicleNumber, supplierNumber: SupplierNumber): Boolean

  def belongsToImport(answers: UserAnswers, vehicleNumber: VehicleNumber, importNumber: ImportNumber): Boolean

  def limitReached(answers: UserAnswers): Boolean

  def deleteValues(answers: UserAnswers, vehicleNumber: VehicleNumber): Future[UserAnswers]

  // returns the changed answers without saving, deleteValues from SupplierService writes both
  def deleteValuesForSupplier(answers: UserAnswers, supplierNumber: SupplierNumber): Try[UserAnswers]

  // returns the changed answers without saving, deleteValues from ImportService writes both
  def deleteValuesForImport(answers: UserAnswers, importNumber: ImportNumber): Try[UserAnswers]

  def count(answers: UserAnswers): Int

  def inOrder(answers: UserAnswers): Seq[(VehicleNumber, JsObject)]
}

@Singleton
class VehicleServiceImpl @Inject() (
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends VehicleService
    with Logging {

  import VehicleServiceImpl.*

  private val MaxVehicles = 100

  def addForSupplier(answers: UserAnswers, supplierNumber: SupplierNumber): Future[VehicleNumber] =
    add(answers, Json.obj(UserAnswers.VehicleSupplierNumberKey -> supplierNumber.value))

  def addForImport(answers: UserAnswers, importNumber: ImportNumber): Future[VehicleNumber] =
    add(answers, Json.obj(UserAnswers.VehicleImportNumberKey -> importNumber.value))

  def numberExists(answers: UserAnswers, vehicleNumber: VehicleNumber): Boolean =
    allVehicles(answers).get(vehicleNumber.value.toString).exists(vehicle => !isDeleted(vehicle))

  def numberHasValues(answers: UserAnswers, vehicleNumber: VehicleNumber): Boolean =
    vehiclesWithValues(answers).contains(vehicleNumber.value.toString)

  def belongsToSupplier(answers: UserAnswers, vehicleNumber: VehicleNumber, supplierNumber: SupplierNumber): Boolean =
    allVehicles(answers)
      .get(vehicleNumber.value.toString)
      .exists(vehicle => (vehicle \ UserAnswers.VehicleSupplierNumberKey).asOpt[Int].contains(supplierNumber.value))

  def belongsToImport(answers: UserAnswers, vehicleNumber: VehicleNumber, importNumber: ImportNumber): Boolean =
    allVehicles(answers)
      .get(vehicleNumber.value.toString)
      .exists(vehicle => (vehicle \ UserAnswers.VehicleImportNumberKey).asOpt[Int].contains(importNumber.value))

  def limitReached(answers: UserAnswers): Boolean = {
    val reached = count(answers) >= MaxVehicles
    if (reached) logger.info(s"Maximum vehicle count of $MaxVehicles reached")
    reached
  }

  def deleteValues(answers: UserAnswers, vehicleNumber: VehicleNumber): Future[UserAnswers] = {
    val vehicles = allVehicles(answers)
    val key      = vehicleNumber.value.toString

    // clears the previous values, keeps the numbered key, skips it if the number is not there
    save(answers, if (vehicles.contains(key)) vehicles + (key -> DeletedVehicle) else vehicles)
  }

  def deleteValuesForSupplier(answers: UserAnswers, supplierNumber: SupplierNumber): Try[UserAnswers] =
    deleteValuesForOwner(answers, UserAnswers.VehicleSupplierNumberKey, supplierNumber.value)

  def deleteValuesForImport(answers: UserAnswers, importNumber: ImportNumber): Try[UserAnswers] =
    deleteValuesForOwner(answers, UserAnswers.VehicleImportNumberKey, importNumber.value)

  def count(answers: UserAnswers): Int =
    vehiclesWithValues(answers).size

  def inOrder(answers: UserAnswers): Seq[(VehicleNumber, JsObject)] =
    vehiclesWithValues(answers).toSeq
      .flatMap { case (key, vehicle) => key.toIntOption.map(number => VehicleNumber(number) -> vehicle) }
      .sortBy { case (number, _) => number.value }

  private def deleteValuesForOwner(answers: UserAnswers, ownerKey: String, ownerNumber: Int): Try[UserAnswers] =
    answers.set(
      AllVehiclesQuery,
      allVehicles(answers).map {
        case (key, vehicle) if (vehicle \ ownerKey).asOpt[Int].contains(ownerNumber) && hasValues(vehicle) =>
          key -> DeletedVehicle
        case entry => entry
      }
    )

  // reuses a collection if no answer values set yet, overwriting its supplierNumber or importNumber
  private def add(answers: UserAnswers, belongsTo: JsObject): Future[VehicleNumber] = {
    val vehicles = allVehicles(answers)
    val number   = availableNumber(vehicles).getOrElse(nextNumber(vehicles))
    val updated  = vehicles + (number.value.toString -> belongsTo)

    save(answers, updated).map(_ => number)
  }

  private def save(answers: UserAnswers, vehicles: Map[String, JsObject]): Future[UserAnswers] =
    sessionRepository.setPage(answers, AllVehiclesQuery, vehicles)

  // every vehicle, empty and deleted included
  private def allVehicles(answers: UserAnswers): Map[String, JsObject] =
    answers.get(AllVehiclesQuery).getOrElse(Map.empty)

  private def vehiclesWithValues(answers: UserAnswers): Map[String, JsObject] =
    allVehicles(answers).filter { case (_, vehicle) => hasValues(vehicle) }

  private def hasValues(vehicle: JsObject): Boolean =
    vehicle.keys.exists(key => !ReservedKeys.contains(key))

  private def isDeleted(vehicle: JsObject): Boolean =
    (vehicle \ DeletedKey).asOpt[Boolean].contains(true)

  private def availableNumber(vehicles: Map[String, JsObject]): Option[VehicleNumber] =
    vehicles.toSeq
      .flatMap { case (key, vehicle) => key.toIntOption.filter(_ => !hasValues(vehicle) && !isDeleted(vehicle)) }
      .minOption
      .map(VehicleNumber(_))

  private def nextNumber(vehicles: Map[String, JsObject]): VehicleNumber =
    VehicleNumber(vehicles.keys.flatMap(_.toIntOption).maxOption.getOrElse(0) + 1)
}

object VehicleServiceImpl {

  private[services] val DeletedKey = "deleted"

  private[services] val DeletedVehicle: JsObject = Json.obj(DeletedKey -> true)

  // reserved keys are ignored when checking for values
  private[services] val ReservedKeys: Set[String] = Set(UserAnswers.VehicleSupplierNumberKey, UserAnswers.VehicleImportNumberKey, DeletedKey)
}
