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

package controllers.supplierdetails

import base.SpecBase
import connectors.NovaImportsBackendConnector
import controllers.{routes, supplierdetails}
import forms.SupplierNameFormProvider
import models.{Address, AddressJourney, BusinessOrPrivateIndividual, CheckMode, Country, DraftId, EUCountry, EuMemberStates, NameDetails, NormalMode, SupplierNumber, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atLeastOnce, never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplieraddress.SupplierAddressPage
import pages.sections.supplierdetails.{SupplierBusinessOrIndividualPage, SupplierEuMemberStatesPage, SupplierNamePage}
import play.api.libs.json.Json
import queries.AllSuppliersQuery
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.AddressLookupService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.SupplierNameView

import scala.concurrent.Future

class SupplierNameControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new SupplierNameFormProvider()
  val form         = formProvider()

  lazy val supplierNameRoute = supplierdetails.routes.SupplierNameController.onPageLoad(SupplierNumber(1), NormalMode).url

  val validTitle     = "Mr"
  val validFirstName = "John"
  val validLastName  = "Smith"

  val supplierName: NameDetails = NameDetails(validTitle, validFirstName, validLastName)

  private val supplierOne = SupplierNumber(1)

  private val testEuCountries = Set(EUCountry("FR", "France"))
  private val journeyUrl      = "http://alf.example.com/lookup/journey-123"

  private val existingAddress =
    Address(lines = Seq("12 High Street", "Reading"), postcode = Some("RE12 9GC"), country = Country("GB", "United Kingdom"))

  // A user reaches /supplier-name only after answering IQ1 "Yes" and AVD-S2.0 "Private individual"
  private val requiredPreviousAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .unsafeSet(SupplierBusinessOrIndividualPage(supplierOne), BusinessOrPrivateIndividual.PrivateIndividual)

  private def applicationWithMockRepository(
    userAnswers: UserAnswers,
    supplierNumber: SupplierNumber = supplierOne
  ): (play.api.Application, SessionRepository) = {

    val mockSessionRepository = mock[SessionRepository]
    val mockAlfService        = mock[AddressLookupService]
    val mockBackendConnector  = mock[NovaImportsBackendConnector]
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
    when(mockBackendConnector.getEuMemberStates()(any())) thenReturn Future.successful(Right(EuMemberStates(testEuCountries)))
    when(mockAlfService.initJourney(eqTo(AddressJourney.Supplier(supplierNumber)), eqTo(false), any[Seq[String]])(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(journeyUrl)))

    val application =
      applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[AddressLookupService].toInstance(mockAlfService),
          bind[NovaImportsBackendConnector].toInstance(mockBackendConnector)
        )
        .build()

    (application, mockSessionRepository)
  }

  private def savedAnswers(mockSessionRepository: SessionRepository): UserAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
    verify(mockSessionRepository, atLeastOnce()).set(captor.capture())
    captor.getValue
  }

  "SupplierNameController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(requiredPreviousAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, supplierNameRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SupplierNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, SupplierNumber(1), NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = requiredPreviousAnswers.set(SupplierNamePage(supplierOne), supplierName).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, supplierNameRoute)

        val view = application.injector.instanceOf[SupplierNameView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(supplierName), SupplierNumber(1), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must save the supplier's name and redirect to the ALF journey when valid data is submitted" in {

      val (application, mockSessionRepository) = applicationWithMockRepository(requiredPreviousAnswers)

      running(application) {
        val request =
          FakeRequest(POST, supplierNameRoute)
            .withFormUrlEncodedBody(("title", validTitle), ("firstName", validFirstName), ("lastName", validLastName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyUrl

        val answers = savedAnswers(mockSessionRepository)
        answers.get(SupplierNamePage(supplierOne)) mustEqual Some(supplierName)
        answers.get(SupplierEuMemberStatesPage(supplierOne)) mustEqual Some(testEuCountries.map(_.toCountry()))
      }
    }

    "must save the supplier's name and save the SupplierOrBusiness as PrivateIndividual when answer is submitted in CheckMode" in {

      val existingAnswers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
      val (application, mockSessionRepository) = applicationWithMockRepository(existingAnswers)

      running(application) {
        val request =
          FakeRequest(POST, supplierdetails.routes.SupplierNameController.onPageLoad(SupplierNumber(1), CheckMode).url)
            .withFormUrlEncodedBody(("title", validTitle), ("firstName", validFirstName), ("lastName", validLastName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyUrl

        val answers = savedAnswers(mockSessionRepository)
        answers.get(SupplierNamePage(supplierOne)) mustEqual Some(supplierName)
        answers.get(SupplierBusinessOrIndividualPage(supplierOne)) mustEqual Some(BusinessOrPrivateIndividual.PrivateIndividual)
        answers.get(SupplierEuMemberStatesPage(supplierOne)) mustEqual Some(testEuCountries.map(_.toCountry()))
      }
    }

    "must save the name and redirect back to CYA, without going through ALF, when an address is already on file (CYA change-name edit)" in {

      val answersWithExistingAddress = requiredPreviousAnswers.set(SupplierAddressPage(supplierOne), existingAddress).success.value

      val (application, mockSessionRepository) = applicationWithMockRepository(answersWithExistingAddress)
      val mockAlfService                       = application.injector.instanceOf[AddressLookupService]

      running(application) {
        val request =
          FakeRequest(POST, supplierdetails.routes.SupplierNameController.onSubmit(supplierOne, CheckMode).url)
            .withFormUrlEncodedBody(("title", validTitle), ("firstName", validFirstName), ("lastName", validLastName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual supplierdetails.routes.SupplierDetailsCheckYourAnswersController.onPageLoad(supplierOne).url

        savedAnswers(mockSessionRepository).get(SupplierNamePage(supplierOne)) mustEqual Some(supplierName)
        verify(mockAlfService, never()).initJourney(any(), any(), any())(any())
      }
    }

    "must still redirect to the ALF journey in NormalMode even when an address is already on file (e.g. user navigates back and resubmits)" in {

      val answersWithExistingAddress = requiredPreviousAnswers.set(SupplierAddressPage(supplierOne), existingAddress).success.value

      val (application, mockSessionRepository) = applicationWithMockRepository(answersWithExistingAddress)

      running(application) {
        val request =
          FakeRequest(POST, supplierNameRoute)
            .withFormUrlEncodedBody(("title", validTitle), ("firstName", validFirstName), ("lastName", validLastName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyUrl

        savedAnswers(mockSessionRepository).get(SupplierNamePage(supplierOne)) mustEqual Some(supplierName)
      }
    }

    "must save the answer matching that of the supplier number in the URL" in {

      val answersForSupplierThree = requiredPreviousAnswers
        .set(AllSuppliersQuery, Map("1" -> Json.obj(), "3" -> Json.obj()))
        .success
        .value
        .set(SupplierBusinessOrIndividualPage(SupplierNumber(3)), BusinessOrPrivateIndividual.PrivateIndividual)
        .success
        .value

      val (application, mockSessionRepository) = applicationWithMockRepository(answersForSupplierThree, SupplierNumber(3))

      running(application) {
        val request =
          FakeRequest(POST, supplierdetails.routes.SupplierNameController.onSubmit(SupplierNumber(3), NormalMode).url)
            .withFormUrlEncodedBody(("title", validTitle), ("firstName", validFirstName), ("lastName", validLastName))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER

        val answers = savedAnswers(mockSessionRepository)

        answers.get(SupplierNamePage(SupplierNumber(3))) mustEqual Some(supplierName)
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(requiredPreviousAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, supplierNameRoute)
            .withFormUrlEncodedBody(("title", validTitle), ("firstName", ""), ("lastName", validLastName))

        val boundForm = form.bind(Map("title" -> validTitle, "firstName" -> "", "lastName" -> validLastName))

        val view = application.injector.instanceOf[SupplierNameView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, SupplierNumber(1), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised for a GET if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, supplierNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, supplierNameRoute)
            .withFormUrlEncodedBody(("title", validTitle), ("firstName", validFirstName), ("lastName", validLastName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if draftId is missing" in {

      val answersWithoutDraftId = emptyUserAnswers
        .set(VehicleFromEuPage, true)
        .success
        .value
        .set(SupplierBusinessOrIndividualPage(supplierOne), BusinessOrPrivateIndividual.PrivateIndividual)
        .success
        .value
        .set(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request = FakeRequest(GET, supplierNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if draftId is missing" in {

      val answersWithoutDraftId = emptyUserAnswers
        .set(VehicleFromEuPage, true)
        .success
        .value
        .set(SupplierBusinessOrIndividualPage(supplierOne), BusinessOrPrivateIndividual.PrivateIndividual)
        .success
        .value
        .set(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request =
          FakeRequest(POST, supplierNameRoute)
            .withFormUrlEncodedBody(("title", validTitle), ("firstName", validFirstName), ("lastName", validLastName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if IQ1 was answered No" in {

      val answersIq1No = requiredPreviousAnswers.set(VehicleFromEuPage, false).success.value

      val application = applicationBuilder(userAnswers = Some(answersIq1No)).build()

      running(application) {
        val request = FakeRequest(GET, supplierNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if AVD-S2.0 was answered Business" in {

      val answersForBusinessSupplier =
        requiredPreviousAnswers.set(SupplierBusinessOrIndividualPage(supplierOne), BusinessOrPrivateIndividual.Business).success.value

      val application = applicationBuilder(userAnswers = Some(answersForBusinessSupplier)).build()

      running(application) {
        val request = FakeRequest(GET, supplierNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if AVD-S2.0 was answered Business" in {

      val answersForBusinessSupplier =
        requiredPreviousAnswers.set(SupplierBusinessOrIndividualPage(supplierOne), BusinessOrPrivateIndividual.Business).success.value

      val application = applicationBuilder(userAnswers = Some(answersForBusinessSupplier)).build()

      running(application) {
        val request =
          FakeRequest(POST, supplierNameRoute)
            .withFormUrlEncodedBody(("title", validTitle), ("firstName", validFirstName), ("lastName", validLastName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if AVD-S2.0 has not been answered" in {

      val answersWithoutSupplierType = emptyUserAnswers
        .set(DraftIdPage, DraftId("DRAFT-001"))
        .success
        .value
        .set(VehicleFromEuPage, true)
        .success
        .value
        .set(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(answersWithoutSupplierType)).build()

      running(application) {
        val request = FakeRequest(GET, supplierNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(requiredPreviousAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, supplierdetails.routes.SupplierNameController.onPageLoad(SupplierNumber(2), NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(requiredPreviousAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, supplierdetails.routes.SupplierNameController.onSubmit(SupplierNumber(2), NormalMode).url)
            .withFormUrlEncodedBody(("title", validTitle), ("firstName", validFirstName), ("lastName", validLastName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the user has no suppliers" in {

      val answersWithoutSuppliers = emptyUserAnswers
        .set(DraftIdPage, DraftId("DRAFT-001"))
        .success
        .value
        .set(VehicleFromEuPage, true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(answersWithoutSuppliers)).build()

      running(application) {
        val request = FakeRequest(GET, supplierNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
