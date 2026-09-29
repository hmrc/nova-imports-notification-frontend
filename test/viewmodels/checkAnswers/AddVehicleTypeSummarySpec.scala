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

package viewmodels.checkAnswers

import base.SpecBase
import controllers.vehicledetails.routes
import models.{AddVehicleType, CheckMode, ImportNumber, SupplierNumber, UserAnswers, VehicleNumber}
import pages.sections.vehicledetails.AddVehicleTypePage
import play.api.Application
import play.api.i18n.Messages
import play.api.libs.json.Json
import queries.AllVehiclesQuery

class AddVehicleTypeSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "AddVehicleTypeSummary" - {

    "must return a summary row with the change link to AVD8.0 for supplier 2 vehicle 1 when the answer is Car" in {

      val userAnswers =
        UserAnswers(userAnswersId)
          .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 2)))
          .unsafeSet(AddVehicleTypePage(VehicleNumber(1)), AddVehicleType.Car)

      val result = AddVehicleTypeSummary.row(userAnswers, VehicleNumber(1)).value

      result.key.content.asHtml.toString   must include(msgs("addVehicleType.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include(msgs("addVehicleType.radio.car"))
      result.actions.value.items.head.href mustBe
        routes.AddVehicleTypeController.supplierOnPageLoad(SupplierNumber(2), VehicleNumber(1), CheckMode).url
    }

    "must return a summary row with the change link to AVD8.0 for import 3 vehicle 4 when the answer is Hcv" in {

      val userAnswers =
        UserAnswers(userAnswersId)
          .unsafeSet(AllVehiclesQuery, Map("4" -> Json.obj("importNumber" -> 3)))
          .unsafeSet(AddVehicleTypePage(VehicleNumber(4)), AddVehicleType.Hcv)

      val result = AddVehicleTypeSummary.row(userAnswers, VehicleNumber(4)).value

      result.key.content.asHtml.toString   must include(msgs("addVehicleType.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include(msgs("addVehicleType.radio.hcv"))
      result.actions.value.items.head.href mustBe
        routes.AddVehicleTypeController.importOnPageLoad(ImportNumber(3), VehicleNumber(4), CheckMode).url
    }

    "must return None when the answer is not present" in {

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))

      AddVehicleTypeSummary.row(userAnswers, VehicleNumber(1)) mustBe None
    }

    "must return None when vehicle 1 has no supplier number and no import number" in {

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(AddVehicleTypePage(VehicleNumber(1)), AddVehicleType.Lcv)

      AddVehicleTypeSummary.row(userAnswers, VehicleNumber(1)) mustBe None
    }
  }
}
