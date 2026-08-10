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

package controllers

import base.SpecBase
import models.{Address, Country, DraftId, SupplierNumber, UserAnswers}
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplierDetails.{SupplierAddressPage, SupplierNumberPage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.SupplierAddressChangedView

class SupplierAddressChangedControllerSpec extends SpecBase {

  private val supplierNumber = SupplierNumber(1)

  private lazy val onPageLoadRoute: String = routes.SupplierAddressChangedController.onPageLoad(supplierNumber).url

  private val address = Address(
    lines = Seq("23 North Road", "East London", "London"),
    postcode = Some("ER45 6UI"),
    country = Country("GB", "United Kingdom")
  )

  private val answersSatisfyingGuard: UserAnswers =
    emptyUserAnswers
      .set(DraftIdPage, DraftId("DRAFT-001"))
      .success
      .value
      .set(VehicleFromEuPage, true)
      .success
      .value
      .set(SupplierNumberPage, 1)
      .success
      .value
      .set(SupplierAddressPage, address)
      .success
      .value

  "SupplierAddressChanged Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(answersSatisfyingGuard)).build()

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[SupplierAddressChangedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(address, supplierNumber)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised for a GET when no supplier address has been captured" in {
      // The guard requires a captured supplier address, so access is blocked before the page renders.
      val answersWithoutAddress = answersSatisfyingGuard.remove(SupplierAddressPage).success.value
      val application           = applicationBuilder(userAnswers = Some(answersWithoutAddress)).build()

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when the supplier number is not in session" in {
      val answersOtherSupplier = answersSatisfyingGuard.set(SupplierNumberPage, 2).success.value
      val application          = applicationBuilder(userAnswers = Some(answersOtherSupplier)).build()

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when the vehicle is not brought into NI from the EU" in {
      val answersNotFromEu = answersSatisfyingGuard.set(VehicleFromEuPage, false).success.value
      val application       = applicationBuilder(userAnswers = Some(answersNotFromEu)).build()

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if no session data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must reject a supplier number below 1 in the URL" in {
      val application = applicationBuilder(userAnswers = Some(answersSatisfyingGuard)).build()

      running(application) {
        val request = FakeRequest(GET, "/nova-imports/supplier/0/supplier-address-changed")
        val result  = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }
  }
}
