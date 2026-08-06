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
import models.{BusinessOrPrivateIndividual, NameDetails}
import pages.sections.supplierDetails.{SupplierBusinessOrIndividualPage, SupplierNamePage}

class SupplierBusinessOrIndividualPageSpec extends SpecBase {

  private val supplierName = NameDetails("Mr", "Test", "McTester")

  "SupplierBusinessOrIndividualPage" - {

    "cleanup" - {

      "must remove the supplier name (AVD-S4.0) when the type is changed to Business" in {
        val userAnswers = emptyUserAnswers
          .set(SupplierBusinessOrIndividualPage, BusinessOrPrivateIndividual.PrivateIndividual)
          .success
          .value
          .set(SupplierNamePage, supplierName)
          .success
          .value

        val result = userAnswers.set(SupplierBusinessOrIndividualPage, BusinessOrPrivateIndividual.Business).success.value

        result.get(SupplierNamePage) mustBe None
      }

      "must keep the supplier name when the type is set to Private individual" in {
        val userAnswers = emptyUserAnswers.set(SupplierNamePage, supplierName).success.value

        val result = userAnswers.set(SupplierBusinessOrIndividualPage, BusinessOrPrivateIndividual.PrivateIndividual).success.value

        result.get(SupplierNamePage) mustBe Some(supplierName)
      }
    }

    "must store the answer under the supplier-details section" in {
      val answers = emptyUserAnswers.unsafeSet(SupplierBusinessOrIndividualPage, BusinessOrPrivateIndividual.PrivateIndividual)

      (answers.data \ "supplier-details" \ "supplierBusinessOrIndividual").as[String] mustBe
        BusinessOrPrivateIndividual.PrivateIndividual.toString
    }
  }
}
