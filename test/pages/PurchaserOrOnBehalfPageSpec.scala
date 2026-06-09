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
import models.{PurchaserBusinessOrIndividual, PurchaserOrOnBehalf}
import pages.sections.initialquestions.{PurchaserBusinessOrIndividualPage, PurchaserOrOnBehalfPage}

class PurchaserOrOnBehalfPageSpec extends SpecBase {

  "PurchaserOrOnBehalfPage" - {

    "cleanup" - {

      "must remove the answer to IQ3.1 when the answer to IQ3.0 is changed to Purchaser" in {
        val userAnswers = emptyUserAnswers
          .set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
          .success
          .value
          .set(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredBusiness)
          .success
          .value

        val result = userAnswers.set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser).success.value

        result.get(PurchaserBusinessOrIndividualPage) mustBe None
      }

      "must not remove the answer to IQ3.1 when the answer to IQ3.0 is OnBehalfOfPurchaser" in {
        val userAnswers = emptyUserAnswers
          .set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
          .success
          .value
          .set(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredBusiness)
          .success
          .value

        val result = userAnswers.set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser).success.value

        result.get(PurchaserBusinessOrIndividualPage) mustBe Some(PurchaserBusinessOrIndividual.NonVatRegisteredBusiness)
      }
    }
  }
}
