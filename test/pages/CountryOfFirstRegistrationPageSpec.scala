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

package pages

import base.SpecBase
import models.VehicleNumber
import pages.sections.vehicledetails.CountryOfFirstRegistrationPage

class CountryOfFirstRegistrationPageSpec extends SpecBase {

  "CountryOfFirstRegistrationPage" - {

    "must store the country code under the vehicle's details" in {
      val answers = emptyUserAnswers.unsafeSet(CountryOfFirstRegistrationPage(VehicleNumber(2)), "FR")

      (answers.data \ "vehicles" \ "2" \ "details" \ "countryOfFirstRegistration").as[String] mustBe "FR"
    }
  }
}
