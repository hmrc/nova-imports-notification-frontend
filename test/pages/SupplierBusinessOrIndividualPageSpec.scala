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
import models.{BusinessOrPrivateIndividual, NameDetails, SupplierNumber}
import pages.sections.supplierdetails.{SupplierBusinessNamePage, SupplierBusinessOrIndividualPage, SupplierNamePage}

class SupplierBusinessOrIndividualPageSpec extends SpecBase {

  private val supplierName = NameDetails("Mr", "Test", "McTester")
  private val supplierOne  = SupplierNumber(1)
  private val supplierTwo  = SupplierNumber(2)

  "SupplierBusinessOrIndividualPage" - {

    "cleanup" - {

      "must remove the supplier name (AVD-S4.0) when the type is changed to Business" in {
        val userAnswers = emptyUserAnswers
          .set(SupplierBusinessOrIndividualPage(supplierOne), BusinessOrPrivateIndividual.PrivateIndividual)
          .success
          .value
          .set(SupplierNamePage(supplierOne), supplierName)
          .success
          .value

        val result = userAnswers.set(SupplierBusinessOrIndividualPage(supplierOne), BusinessOrPrivateIndividual.Business).success.value

        result.get(SupplierNamePage(supplierOne)) mustBe None
      }

      "must keep the supplier name when the type is set to Private individual" in {
        val userAnswers = emptyUserAnswers.set(SupplierNamePage(supplierOne), supplierName).success.value

        val result =
          userAnswers.set(SupplierBusinessOrIndividualPage(supplierOne), BusinessOrPrivateIndividual.PrivateIndividual).success.value

        result.get(SupplierNamePage(supplierOne)) mustBe Some(supplierName)
      }

      "must remove the supplier business name when the type is changed to Private individual" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(SupplierBusinessOrIndividualPage(supplierOne), BusinessOrPrivateIndividual.Business)
          .unsafeSet(SupplierBusinessNamePage(supplierOne), "Acme Trading Co Ltd")

        val result = userAnswers.unsafeSet(SupplierBusinessOrIndividualPage(supplierOne), BusinessOrPrivateIndividual.PrivateIndividual)

        result.get(SupplierBusinessNamePage(supplierOne)) mustBe None
      }

      "must keep the supplier business name when the type is set to Business" in {
        val userAnswers = emptyUserAnswers.unsafeSet(SupplierBusinessNamePage(supplierOne), "Acme Trading Co Ltd")

        val result = userAnswers.unsafeSet(SupplierBusinessOrIndividualPage(supplierOne), BusinessOrPrivateIndividual.Business)

        result.get(SupplierBusinessNamePage(supplierOne)) mustBe Some("Acme Trading Co Ltd")
      }

      "must only clear supplier 1's business name when supplier 1 changes to Private individual" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(SupplierBusinessNamePage(supplierOne), "First Ltd")
          .unsafeSet(SupplierBusinessNamePage(supplierTwo), "Second Ltd")

        val result = userAnswers.unsafeSet(SupplierBusinessOrIndividualPage(supplierOne), BusinessOrPrivateIndividual.PrivateIndividual)

        result.get(SupplierBusinessNamePage(supplierOne)) mustBe None
        result.get(SupplierBusinessNamePage(supplierTwo)) mustBe Some("Second Ltd")
      }
    }

    "must store the answer under the supplier it belongs to" in {
      val answers =
        emptyUserAnswers.unsafeSet(SupplierBusinessOrIndividualPage(supplierTwo), BusinessOrPrivateIndividual.PrivateIndividual)

      (answers.data \ "suppliers" \ "2" \ "details" \ "supplierBusinessOrIndividual").as[String] mustBe
        BusinessOrPrivateIndividual.PrivateIndividual.toString
    }
  }
}
