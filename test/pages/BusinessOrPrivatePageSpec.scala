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
import models.{BusinessOrPrivateIndividual, ContactNumbers, NameDetails}
import pages.sections.initialquestions.BusinessOrPrivatePage
import pages.sections.notifierdetails.{BusinessNamePage, EmailAddressPage, NameDetailsPage, PhoneNumberPage}

class BusinessOrPrivatePageSpec extends SpecBase {

  private val name           = NameDetails("Mr", "Test", "McTester")
  private val businessName   = "The Business"
  private val contactNumbers = ContactNumbers(Some("01632 960 001"), None)
  private val email          = "name@example.com"

  "BusinessOrPrivatePage" - {

    "cleanup" - {

      "must remove the individual name when the type is changed to Business" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
          .unsafeSet(NameDetailsPage, name)

        val result = userAnswers.unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)

        result.get(NameDetailsPage) mustBe None
      }

      "must remove the business name when the type is changed to Private individual" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
          .unsafeSet(BusinessNamePage, businessName)

        val result = userAnswers.unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)

        result.get(BusinessNamePage) mustBe None
      }

      "must keep the phone number and email when the type is changed" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
          .unsafeSet(NameDetailsPage, name)
          .unsafeSet(PhoneNumberPage, contactNumbers)
          .unsafeSet(EmailAddressPage, email)

        val result = userAnswers.unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)

        result.get(PhoneNumberPage) mustBe Some(contactNumbers)
        result.get(EmailAddressPage) mustBe Some(email)
      }

      "must keep the individual name when the type is re-set to Private individual" in {
        val userAnswers = emptyUserAnswers.unsafeSet(NameDetailsPage, name)

        val result = userAnswers.unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)

        result.get(NameDetailsPage) mustBe Some(name)
      }

      "must keep the business name when the type is re-set to Business" in {
        val userAnswers = emptyUserAnswers.unsafeSet(BusinessNamePage, businessName)

        val result = userAnswers.unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)

        result.get(BusinessNamePage) mustBe Some(businessName)
      }
    }
  }
}
