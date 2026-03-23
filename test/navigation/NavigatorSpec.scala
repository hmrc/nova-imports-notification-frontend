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

package navigation

import base.SpecBase
import controllers.routes
import pages.*
import models.*

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id")) mustBe routes.IndexController.onPageLoad()
      }

      "must go from VehicleFromEuPage to BusinessPrivateController when the answer is true" in {

        val userAnswers = UserAnswers("id").set(VehicleFromEuPage, true).success.value
        navigator.nextPage(VehicleFromEuPage, NormalMode, userAnswers) mustBe routes.BusinessPrivateController.onPageLoad(NormalMode)
      }

      "must go from VehicleFromEuPage to VehicleOutsideEUController when the answer is false" in {

        val userAnswers = UserAnswers("id").set(VehicleFromEuPage, false).success.value
        navigator.nextPage(VehicleFromEuPage, NormalMode, userAnswers) mustBe routes.VehicleOutsideEUController.onPageLoad()
      }

      "must go from VehicleFromEuPage to Journey Recovery when there is no answer" in {

        navigator.nextPage(VehicleFromEuPage, NormalMode, UserAnswers("id")) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from BusinessPrivatePage to Index" in {

        navigator.nextPage(BusinessPrivatePage, NormalMode, UserAnswers("id")) mustBe routes.IndexController.onPageLoad()
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, UserAnswers("id")) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from VehicleFromEuPage to CheckYourAnswers" in {

        val userAnswers = UserAnswers("id").set(VehicleFromEuPage, true).success.value
        navigator.nextPage(VehicleFromEuPage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from BusinessPrivatePage to CheckYourAnswers" in {

        navigator.nextPage(BusinessPrivatePage, CheckMode, UserAnswers("id")) mustBe routes.CheckYourAnswersController.onPageLoad()
      }
    }
  }
}
