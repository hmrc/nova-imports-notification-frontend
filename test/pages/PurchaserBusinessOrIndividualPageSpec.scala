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
import models.{NameDetails, PurchaserBusinessOrIndividual}
import pages.sections.initialquestions.PurchaserBusinessOrIndividualPage
import pages.sections.purchaserDetails.{PurchaserBusinessNamePage, PurchaserNamePage}

class PurchaserBusinessOrIndividualPageSpec extends SpecBase {

  "PurchaserBusinessOrIndividualPage" - {

    "cleanup" - {

      "must remove the purchaser name (APD1.0) when the type is changed to Business" in {
        val userAnswers = emptyUserAnswers
          .set(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredPrivateIndividual)
          .success
          .value
          .set(PurchaserNamePage, NameDetails("Mr", "Test", "McTester"))
          .success
          .value

        val result = userAnswers.set(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredBusiness).success.value

        result.get(PurchaserNamePage) mustBe None
      }

      "must remove the purchaser business name (APD2.0) when the type is changed to Private individual" in {
        val userAnswers = emptyUserAnswers
          .set(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredBusiness)
          .success
          .value
          .set(PurchaserBusinessNamePage, "The Business")
          .success
          .value

        val result =
          userAnswers.set(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredPrivateIndividual).success.value

        result.get(PurchaserBusinessNamePage) mustBe None
      }

      "must keep the business name when the type is set to Business" in {
        val userAnswers = emptyUserAnswers
          .set(PurchaserBusinessNamePage, "The Business")
          .success
          .value

        val result = userAnswers.set(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredBusiness).success.value

        result.get(PurchaserBusinessNamePage) mustBe Some("The Business")
      }

      "must keep the purchaser name when the type is set to Private individual" in {
        val userAnswers = emptyUserAnswers
          .set(PurchaserNamePage, NameDetails("Mr", "Test", "McTester"))
          .success
          .value

        val result =
          userAnswers.set(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredPrivateIndividual).success.value

        result.get(PurchaserNamePage) mustBe Some(NameDetails("Mr", "Test", "McTester"))
      }
    }
  }
}
