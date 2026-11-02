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

package controllers.vehicledetails

import base.SpecBase
import com.google.inject.name.Names
import controllers.actions.*
import controllers.{routes, vehicledetails}
import forms.NumberOfVehiclesFormProvider
import models.*
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.vehicledetails.*
import pages.{AgentSelectedClientPage, DraftIdPage}
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import viewmodels.VehicleDetailsInset
import views.html.NumberOfVehiclesView

import scala.concurrent.Future

class NumberOfVehiclesControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val form = new NumberOfVehiclesFormProvider()()

  val supplierNumber = SupplierNumber(1)
  val importNumber   = ImportNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  val oneVehicleAnswer       = NumberOfVehicles(OneOrMultipleVehicles.One, None)
  val multipleVehiclesAnswer = NumberOfVehicles(OneOrMultipleVehicles.Multiple, Some(5))

  lazy val supplierRoute = vehicledetails.routes.NumberOfVehiclesController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode).url
  lazy val importRoute   = vehicledetails.routes.NumberOfVehiclesController.importOnPageLoad(importNumber, vehicleNumber, NormalMode).url

  private def withVehicleDetails(answers: UserAnswers): UserAnswers =
    answers
      .unsafeSet(AddVehicleTypePage(vehicleNumber), AddVehicleType.Hcv)
      .unsafeSet(AddVehicleDetailsHeavyCommercialPage(vehicleNumber), AddVehicleDetailsHeavyCommercial("Make", "Model", "HcvType", "CabType"))

  val supplierAnswers: UserAnswers = withVehicleDetails(
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(AmendSubmittedNotificationPage, false)
      .unsafeSet(VehicleFromEuPage, true)
      .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
      .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))))
  )

  val importAnswers: UserAnswers = withVehicleDetails(
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(AmendSubmittedNotificationPage, false)
      .unsafeSet(VehicleFromEuPage, false)
      .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123-456789A")))
      .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))))
  )

  val answersWithTwoVehicles: UserAnswers = withVehicleDetails(
    supplierAnswers.unsafeSet(
      AllVehiclesQuery,
      Map(
        "1" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true)),
        "2" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))
      )
    )
  )

  private def formFields(oneOrMultiple: String, number: String = "") =
    Seq("oneOrMultipleVehicles" -> oneOrMultiple, "numberOfVehiclesToMultiply" -> number)

  private def supplierSubmitCall(mode: Mode): Call =
    vehicledetails.routes.NumberOfVehiclesController.supplierOnSubmit(supplierNumber, vehicleNumber, mode)

  private def importSubmitCall(mode: Mode): Call =
    vehicledetails.routes.NumberOfVehiclesController.importOnSubmit(importNumber, vehicleNumber, mode)

  private def applicationForUserType(identifierAction: Class[? <: IdentifierAction], userAnswers: UserAnswers): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswers)))
      )
      .build()

  private def applicationWithMockRepository(userAnswers: UserAnswers): (Application, SessionRepository) = {

    val mockSessionRepository = mock[SessionRepository]
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

    val application =
      applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

    (application, mockSessionRepository)
  }

  private def savedAnswers(mockSessionRepository: SessionRepository): UserAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
    verify(mockSessionRepository).set(captor.capture())
    captor.getValue
  }

  private def assertUnauthorisedOnGet(application: Application, url: String) =
    running(application) {
      val result = route(application, FakeRequest(GET, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }

  private def assertUnauthorisedOnPost(application: Application, url: String) =
    running(application) {
      val result = route(application, FakeRequest(POST, url).withFormUrlEncodedBody(formFields("One")*)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }

  private def assertBadRequestWithError(userAnswers: UserAnswers, url: String, fields: Seq[(String, String)])(expectedError: Messages => String) = {

    val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

    running(application) {
      val result = route(application, FakeRequest(POST, url).withFormUrlEncodedBody(fields*)).value

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) must include(expectedError(messages(application)))
    }
  }

  "NumberOfVehiclesController" - {

    "on the supplier journey" - {

      "must return OK and the correct view with the vehicle details for a GET" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[NumberOfVehiclesView]
          val msgs    = messages(application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            VehicleDetailsInset(supplierAnswers, vehicleNumber)(msgs),
            supplierSubmitCall(NormalMode)
          )(request, msgs).toString
          contentAsString(result) must include("<b>Make Model</b>")
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val answers     = supplierAnswers.unsafeSet(NumberOfVehiclesPage(vehicleNumber), multipleVehiclesAnswer)
        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[NumberOfVehiclesView]
          val msgs    = messages(application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(multipleVehiclesAnswer),
            VehicleDetailsInset(answers, vehicleNumber)(msgs),
            supplierSubmitCall(NormalMode)
          )(request, msgs).toString
        }
      }

      "must post back to the change URL for a GET in CheckMode" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, supplierSubmitCall(CheckMode).url)).value

          status(result) mustEqual OK
          contentAsString(result) must include(s"""action="${supplierSubmitCall(CheckMode).url}"""")
        }
      }

      "must redirect to the next page and save the answer when one vehicle is selected" in {
        val (application, mockSessionRepository) = applicationWithMockRepository(supplierAnswers)

        running(application) {
          val result = route(application, FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(formFields("One")*)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          savedAnswers(mockSessionRepository).get(NumberOfVehiclesPage(vehicleNumber)) mustEqual Some(oneVehicleAnswer)
        }
      }

      "must redirect to the next page and save both answers when more than one vehicle is selected" in {
        val (application, mockSessionRepository) = applicationWithMockRepository(supplierAnswers)

        running(application) {
          val result = route(application, FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(formFields("Multiple", "5")*)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          savedAnswers(mockSessionRepository).get(NumberOfVehiclesPage(vehicleNumber)) mustEqual Some(multipleVehiclesAnswer)
        }
      }

      "must remove the number when the answer is changed to one vehicle" in {
        val answers                              = supplierAnswers.unsafeSet(NumberOfVehiclesPage(vehicleNumber), multipleVehiclesAnswer)
        val (application, mockSessionRepository) = applicationWithMockRepository(answers)

        running(application) {
          val result = route(application, FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(formFields("One")*)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          savedAnswers(mockSessionRepository).get(NumberOfVehiclesPage(vehicleNumber)) mustEqual Some(oneVehicleAnswer)
        }
      }

      "must return a Bad Request and an error when nothing is selected" in {
        assertBadRequestWithError(supplierAnswers, supplierRoute, formFields(""))(msgs => msgs("numberOfVehicles.error.required"))
      }

      "must return a Bad Request and an error when more than one vehicle is selected and the number is blank" in {
        assertBadRequestWithError(supplierAnswers, supplierRoute, formFields("Multiple"))(msgs => msgs("numberOfVehicles.error.input.required"))
      }

      "must return a Bad Request and an error when the number is not between 2 and 100" in {
        assertBadRequestWithError(supplierAnswers, supplierRoute, formFields("Multiple", "101"))(msgs => msgs("numberOfVehicles.error.input.format"))
      }

      "must return OK for a GET from a private individual" in {
        val application = applicationForUserType(classOf[FakeIdentifierAction], supplierAnswers)

        running(application) {
          status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from a non-VAT organisation" in {
        val application = applicationForUserType(classOf[FakeOrganisationIdentifierAction], supplierAnswers)

        running(application) {
          status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from a VAT organisation" in {
        val application = applicationForUserType(classOf[FakeVatTraderIdentifierAction], supplierAnswers)

        running(application) {
          status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent with the agent enrolment" in {
        val application = applicationForUserType(classOf[FakeAgentIdentifierAction], supplierAnswers)

        running(application) {
          status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent without agent enrolment" in {
        val application = applicationForUserType(classOf[FakeAgentNoEnrolmentsIdentifierAction], supplierAnswers)

        running(application) {
          status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent with a client selected" in {
        val answers     = supplierAnswers.unsafeSet(AgentSelectedClientPage, AgentSelectedClient("123456789", Some("Company 1 Ltd")))
        val application = applicationForUserType(classOf[FakeAgentIdentifierAction], answers)

        running(application) {
          status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must redirect to Unauthorised for a GET if no existing session data is found" in {
        assertUnauthorisedOnGet(applicationBuilder(userAnswers = None).build(), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if no draft is in the session" in {
        val answers = supplierAnswers.remove(DraftIdPage).success.value
        assertUnauthorisedOnGet(applicationBuilder(userAnswers = Some(answers)).build(), supplierRoute)
      }

      "must redirect to Unauthorised for a POST if no draft is in the session" in {
        val answers = supplierAnswers.remove(DraftIdPage).success.value
        assertUnauthorisedOnPost(applicationBuilder(userAnswers = Some(answers)).build(), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if the vehicle was not brought from the EU" in {
        val answers = supplierAnswers.unsafeSet(VehicleFromEuPage, false)
        assertUnauthorisedOnGet(applicationBuilder(userAnswers = Some(answers)).build(), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {
        assertUnauthorisedOnGet(
          applicationBuilder(userAnswers = Some(supplierAnswers)).build(),
          vehicledetails.routes.NumberOfVehiclesController.supplierOnPageLoad(SupplierNumber(2), vehicleNumber, NormalMode).url
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different supplier" in {
        val answers = supplierAnswers
          .unsafeSet(
            AllSuppliersQuery,
            Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false), "2" -> Json.obj("usePersonalDetailsAsSupplier" -> false))
          )
          .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 2, "details" -> Json.obj("bringingVehicleNI" -> true))))

        assertUnauthorisedOnGet(applicationBuilder(userAnswers = Some(answers)).build(), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if the user is updating a submitted notification" in {
        val answers = supplierAnswers.unsafeSet(AmendSubmittedNotificationPage, true)
        assertUnauthorisedOnGet(applicationBuilder(userAnswers = Some(answers)).build(), supplierRoute)
      }

      "must redirect to Unauthorised for a GET when the vehicle has no answers yet" in {
        val answers = supplierAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
        assertUnauthorisedOnGet(applicationBuilder(userAnswers = Some(answers)).build(), supplierRoute)
      }
    }

    "on the import journey" - {

      "must return OK and the correct view with the vehicle details for a GET" in {
        val application = applicationBuilderWithVatTrader(userAnswers = Some(importAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, importRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[NumberOfVehiclesView]
          val msgs    = messages(application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            VehicleDetailsInset(importAnswers, vehicleNumber)(msgs),
            importSubmitCall(NormalMode)
          )(request, msgs).toString
        }
      }

      "must post back to the change URL for a GET in CheckMode" in {
        val application = applicationBuilderWithVatTrader(userAnswers = Some(importAnswers)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, importSubmitCall(CheckMode).url)).value

          status(result) mustEqual OK
          contentAsString(result) must include(s"""action="${importSubmitCall(CheckMode).url}"""")
        }
      }

      "must redirect to the next page and save both answers when more than one vehicle is selected" in {
        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilderWithVatTrader(userAnswers = Some(importAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val result = route(application, FakeRequest(POST, importRoute).withFormUrlEncodedBody(formFields("Multiple", "5")*)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          savedAnswers(mockSessionRepository).get(NumberOfVehiclesPage(vehicleNumber)) mustBe Some(multipleVehiclesAnswer)
        }
      }

      "must return a Bad Request and an error when nothing is selected" in {
        val application = applicationBuilderWithVatTrader(userAnswers = Some(importAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody(formFields("")*)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[NumberOfVehiclesView]
          val msgs    = messages(application)

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            form.bind(formFields("").toMap),
            VehicleDetailsInset(importAnswers, vehicleNumber)(msgs),
            importSubmitCall(NormalMode)
          )(request, msgs).toString
        }
      }

      "must return OK for a GET from a VAT organisation" in {
        val application = applicationForUserType(classOf[FakeVatTraderIdentifierAction], importAnswers)

        running(application) {
          status(route(application, FakeRequest(GET, importRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent with the agent enrolment" in {
        val application = applicationForUserType(classOf[FakeAgentIdentifierAction], importAnswers)

        running(application) {
          status(route(application, FakeRequest(GET, importRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent without agent enrolment" in {
        val application = applicationForUserType(classOf[FakeAgentNoEnrolmentsIdentifierAction], importAnswers)

        running(application) {
          status(route(application, FakeRequest(GET, importRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent with a client selected" in {
        val answers     = importAnswers.unsafeSet(AgentSelectedClientPage, AgentSelectedClient("123456789", Some("Company 1 Ltd")))
        val application = applicationForUserType(classOf[FakeAgentIdentifierAction], answers)

        running(application) {
          status(route(application, FakeRequest(GET, importRoute)).value) mustEqual OK
        }
      }

      "must redirect to Unauthorised for a private individual" in {
        assertUnauthorisedOnGet(applicationForUserType(classOf[FakeIdentifierAction], importAnswers), importRoute)
      }

      "must redirect to Unauthorised for a non-VAT organisation" in {
        assertUnauthorisedOnGet(applicationForUserType(classOf[FakeOrganisationIdentifierAction], importAnswers), importRoute)
      }

      "must redirect to Unauthorised for a GET if no existing session data is found" in {
        assertUnauthorisedOnGet(applicationBuilderWithVatTrader(userAnswers = None).build(), importRoute)
      }

      "must redirect to Unauthorised for a GET if no draft is in progress" in {
        val answers = importAnswers.remove(DraftIdPage).success.value
        assertUnauthorisedOnGet(applicationBuilderWithVatTrader(userAnswers = Some(answers)).build(), importRoute)
      }

      "must redirect to Unauthorised for a POST if no draft is in progress" in {
        val answers = importAnswers.remove(DraftIdPage).success.value
        assertUnauthorisedOnPost(applicationBuilderWithVatTrader(userAnswers = Some(answers)).build(), importRoute)
      }

      "must redirect to Unauthorised for a GET if the vehicle was brought from the EU" in {
        val answers = importAnswers.unsafeSet(VehicleFromEuPage, true)
        assertUnauthorisedOnGet(applicationBuilderWithVatTrader(userAnswers = Some(answers)).build(), importRoute)
      }

      "must redirect to Unauthorised for a GET if the import number in the URL is not one of the user's imports" in {
        assertUnauthorisedOnGet(
          applicationBuilderWithVatTrader(userAnswers = Some(importAnswers)).build(),
          vehicledetails.routes.NumberOfVehiclesController.importOnPageLoad(ImportNumber(2), vehicleNumber, NormalMode).url
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different import" in {
        val answers = importAnswers
          .unsafeSet(
            AllImportsQuery,
            Map("1" -> Json.obj("importEntryNumber" -> "123-456789A"), "2" -> Json.obj("importEntryNumber" -> "987-654321B"))
          )
          .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 2, "details" -> Json.obj("bringingVehicleNI" -> true))))

        assertUnauthorisedOnGet(applicationBuilderWithVatTrader(userAnswers = Some(answers)).build(), importRoute)
      }

      "must redirect to Unauthorised for a GET when the vehicle has no answers yet" in {
        val answers = importAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1)))
        assertUnauthorisedOnGet(applicationBuilderWithVatTrader(userAnswers = Some(answers)).build(), importRoute)
      }
    }

    "new notification with two vehicles in it" - {

//      TODO: Update these landing page redirects to ERR5 when built
      "must redirect to the landing page and not save the answer when more than one vehicle is selected" in {
        val (application, mockSessionRepository) = applicationWithMockRepository(answersWithTwoVehicles)

        running(application) {
          val result = route(application, FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(formFields("Multiple", "5")*)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url
          verify(mockSessionRepository, never()).set(any())
        }
      }

      "must redirect to the landing page and not save the answer when more than one vehicle is selected on the change URL" in {
        val (application, mockSessionRepository) = applicationWithMockRepository(answersWithTwoVehicles)

        running(application) {
          val request = FakeRequest(POST, supplierSubmitCall(CheckMode).url).withFormUrlEncodedBody(formFields("Multiple", "5")*)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url
          verify(mockSessionRepository, never()).set(any())
        }
      }

      "must redirect to the next page and save the answer when one vehicle is selected" in {
        val (application, mockSessionRepository) = applicationWithMockRepository(answersWithTwoVehicles)

        running(application) {
          val result = route(application, FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(formFields("One")*)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          savedAnswers(mockSessionRepository).get(NumberOfVehiclesPage(vehicleNumber)) mustEqual Some(oneVehicleAnswer)
        }
      }
    }
  }
}
