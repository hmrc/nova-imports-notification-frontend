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
import forms.VehicleIdentificationNumberFormProvider
import models.*
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.vehicledetails.{AddVehicleDetailsHeavyCommercialPage, AddVehicleTypePage, VehicleIdentificationNumberPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import viewmodels.VehicleDetailsInset
import views.html.VehicleIdentificationNumberView

import scala.concurrent.Future

class VehicleIdentificationNumberControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val supplierNumber = SupplierNumber(1)
  val importNumber   = ImportNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  val answer = "1ABCD2E03FG456789"

  private val noDuplicates = new VehicleIdentificationNumberFormProvider()(_ => false)

  lazy val supplierRoute =
    vehicledetails.routes.VehicleIdentificationNumberController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode).url

  lazy val importRoute = vehicledetails.routes.VehicleIdentificationNumberController.importOnPageLoad(importNumber, vehicleNumber, NormalMode).url

  lazy val supplierSubmit = vehicledetails.routes.VehicleIdentificationNumberController.supplierOnSubmit(supplierNumber, vehicleNumber, NormalMode)

  lazy val importSubmit = vehicledetails.routes.VehicleIdentificationNumberController.importOnSubmit(importNumber, vehicleNumber, NormalMode)

  private def withVehicleDetails(answers: UserAnswers): UserAnswers =
    answers
      .unsafeSet(AddVehicleTypePage(vehicleNumber), AddVehicleType.Hcv)
      .unsafeSet(
        AddVehicleDetailsHeavyCommercialPage(vehicleNumber),
        AddVehicleDetailsHeavyCommercial("Volvo", "FH16", "Tractor unit", "Sleeper cab")
      )

  private val supplierVehicle: JsObject =
    Json.obj("supplierNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true, "numberOfVehicles" -> Json.obj("oneMultiVehicle" -> "one")))

  private def supplierVehicleWithVin(supplierNumber: Int, vin: String): JsObject =
    Json.obj(
      "supplierNumber" -> supplierNumber,
      "details"        -> Json.obj("vehicleIdentificationNumber" -> vin, "numberOfVehicles" -> Json.obj("oneMultiVehicle" -> "one"))
    )

  val supplierAnswers: UserAnswers = withVehicleDetails(
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(VehicleFromEuPage, true)
      .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
      .unsafeSet(AllVehiclesQuery, Map("1" -> supplierVehicle))
  )

  val importAnswers: UserAnswers = withVehicleDetails(
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(VehicleFromEuPage, false)
      .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123-456789A")))
      .unsafeSet(
        AllVehiclesQuery,
        Map(
          "1" -> Json
            .obj("importNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true, "numberOfVehicles" -> Json.obj("oneMultiVehicle" -> "one")))
        )
      )
  )

  private def applicationForUserType(identifierAction: Class[? <: IdentifierAction], userAnswers: UserAnswers): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswers)))
      )
      .build()

  private def applicationWithMockRepository(builder: GuiceApplicationBuilder): (Application, SessionRepository) = {
    val mockSessionRepository = mock[SessionRepository]
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

    val application = builder
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

  private def mustBeUnauthorised(application: Application, url: String) =
    running(application) {
      val result = route(application, FakeRequest(GET, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }

  "VehicleIdentificationNumberController" - {

    "supplier journey" - {

      "must return OK and the correct view with the vehicle details for a GET" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[VehicleIdentificationNumberView]
          val msgs    = messages(application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            noDuplicates,
            VehicleDetailsInset(supplierAnswers, vehicleNumber)(msgs),
            supplierSubmit
          )(request, msgs).toString
          contentAsString(result) must include("<strong>Volvo FH16</strong>")
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = supplierAnswers.unsafeSet(VehicleIdentificationNumberPage(vehicleNumber), answer)
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[VehicleIdentificationNumberView]
          val msgs    = messages(application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            noDuplicates.fill(answer),
            VehicleDetailsInset(userAnswers, vehicleNumber)(msgs),
            supplierSubmit
          )(request, msgs).toString
        }
      }

      "must redirect to the next page and save the answer when valid data is submitted" in {
        val (application, mockSessionRepository) = applicationWithMockRepository(applicationBuilder(userAnswers = Some(supplierAnswers)))

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", answer))
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(VehicleIdentificationNumberPage(vehicleNumber)) mustEqual Some(answer)
        }
      }

      "must return a Bad Request with the vehicle details and the required error when nothing is entered" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", ""))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("vehicleIdentificationNumber.error.required"))
          contentAsString(result) must include("<strong>Volvo FH16</strong>")
        }
      }

      "must return a Bad Request and the length error when more than 17 characters are entered" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", "1ABCD2E03FG4567890"))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("vehicleIdentificationNumber.error.length"))
        }
      }

      "must return a Bad Request and the format error when disallowed characters are entered" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          List("1ABCD2E03FG 56789", "1ABCD2E03FG-56789").foreach { vin =>
            val result = route(application, FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", vin))).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) must include(messages(application)("vehicleIdentificationNumber.error.invalid"))
          }
        }
      }

      "must return a Bad Request and the duplicate error when another vehicle in the submission has the VIN, ignoring case" in {
        val answers = supplierAnswers.unsafeSet(
          AllVehiclesQuery,
          Map("1" -> supplierVehicle, "2" -> supplierVehicleWithVin(1, answer.toLowerCase))
        )
        val (application, mockSessionRepository) = applicationWithMockRepository(applicationBuilder(userAnswers = Some(answers)))

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", answer))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("vehicleIdentificationNumber.error.duplicate"))
          verify(mockSessionRepository, never()).set(any())
        }
      }

      "must not treat the vehicle's own saved VIN as a duplicate" in {
        val answers = supplierAnswers.unsafeSet(
          AllVehiclesQuery,
          Map("1" -> supplierVehicleWithVin(1, answer), "2" -> supplierVehicleWithVin(1, "1ABCD2E03FG456788"))
        )
        val (application, mockSessionRepository) = applicationWithMockRepository(applicationBuilder(userAnswers = Some(answers)))

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", answer))
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          savedAnswers(mockSessionRepository).get(VehicleIdentificationNumberPage(vehicleNumber)) mustEqual Some(answer)
        }
      }

      "must return OK for a GET for an agent" in {
        val application = applicationForUserType(classOf[FakeAgentIdentifierAction], supplierAnswers)

        running(application) {
          status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must redirect to Unauthorised for a GET if no existing session data is found" in {
        mustBeUnauthorised(applicationBuilder(userAnswers = None).build(), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if draftId is missing" in {
        val answers = supplierAnswers.remove(DraftIdPage).success.value
        mustBeUnauthorised(applicationBuilder(userAnswers = Some(answers)).build(), supplierRoute)
      }

      "must redirect to Unauthorised for a POST if draftId is missing" in {
        val answers     = supplierAnswers.remove(DraftIdPage).success.value
        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", answer))
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a GET if IQ1 was answered No" in {
        val answers = supplierAnswers.unsafeSet(VehicleFromEuPage, false)
        mustBeUnauthorised(applicationBuilder(userAnswers = Some(answers)).build(), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {
        mustBeUnauthorised(
          applicationBuilder(userAnswers = Some(supplierAnswers)).build(),
          vehicledetails.routes.VehicleIdentificationNumberController.supplierOnPageLoad(SupplierNumber(2), vehicleNumber, NormalMode).url
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different supplier" in {
        val answers = supplierAnswers
          .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj(), "2" -> Json.obj()))
          .unsafeSet(
            AllVehiclesQuery,
            Map(
              "1" -> Json.obj(
                "supplierNumber" -> 2,
                "details"        -> Json.obj("bringingVehicleNI" -> true, "numberOfVehicles" -> Json.obj("oneMultiVehicle" -> "one"))
              )
            )
          )

        mustBeUnauthorised(applicationBuilder(userAnswers = Some(answers)).build(), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if the vehicle has no values" in {
        val answers = supplierAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
        mustBeUnauthorised(applicationBuilder(userAnswers = Some(answers)).build(), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if AVD10.0 is not One vehicle" in {
        val answers = supplierAnswers.unsafeSet(
          AllVehiclesQuery,
          Map(
            "1" -> Json.obj(
              "supplierNumber" -> 1,
              "details"        -> Json.obj("numberOfVehicles" -> Json.obj("oneMultiVehicle" -> "more", "numberSameVehicles" -> 2))
            )
          )
        )
        mustBeUnauthorised(applicationBuilder(userAnswers = Some(answers)).build(), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if AVD10.0 has not been answered" in {
        val answers = supplierAnswers.unsafeSet(
          AllVehiclesQuery,
          Map("1" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true)))
        )
        mustBeUnauthorised(applicationBuilder(userAnswers = Some(answers)).build(), supplierRoute)
      }

      "must return OK for a GET in an amend session even though AVD10.0 was skipped" in {
        val answers = supplierAnswers
          .unsafeSet(AmendSubmittedNotificationPage, true)
          .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))))
        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, supplierRoute)).value

          status(result) mustEqual OK
        }
      }

      "must save the answer and redirect for a POST in an amend session even though AVD10.0 was skipped" in {
        val answers = supplierAnswers
          .unsafeSet(AmendSubmittedNotificationPage, true)
          .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))))
        val (application, mockSessionRepository) = applicationWithMockRepository(applicationBuilder(userAnswers = Some(answers)))

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", answer))
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          verify(mockSessionRepository).set(any())
        }
      }
    }

    "import journey" - {

      "must return OK for a GET in an amend session even though AVD10.0 was skipped" in {
        val answers = importAnswers
          .unsafeSet(AmendSubmittedNotificationPage, true)
          .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))))
        val application = applicationBuilderWithVatTrader(userAnswers = Some(answers)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, importRoute)).value

          status(result) mustEqual OK
        }
      }

      "must return OK and the correct view with the vehicle details for a GET" in {
        val application = applicationBuilderWithVatTrader(userAnswers = Some(importAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, importRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[VehicleIdentificationNumberView]
          val msgs    = messages(application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            noDuplicates,
            VehicleDetailsInset(importAnswers, vehicleNumber)(msgs),
            importSubmit
          )(request, msgs).toString
          contentAsString(result) must include("<strong>Volvo FH16</strong>")
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = importAnswers.unsafeSet(VehicleIdentificationNumberPage(vehicleNumber), answer)
        val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, importRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[VehicleIdentificationNumberView]
          val msgs    = messages(application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            noDuplicates.fill(answer),
            VehicleDetailsInset(userAnswers, vehicleNumber)(msgs),
            importSubmit
          )(request, msgs).toString
        }
      }

      "must redirect to the next page and save the answer when valid data is submitted" in {
        val (application, mockSessionRepository) =
          applicationWithMockRepository(applicationBuilderWithVatTrader(userAnswers = Some(importAnswers)))

        running(application) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody(("value", answer))
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          savedAnswers(mockSessionRepository).get(VehicleIdentificationNumberPage(vehicleNumber)) mustEqual Some(answer)
        }
      }

      "must return a Bad Request with the vehicle details and the required error when nothing is entered" in {
        val application = applicationBuilderWithVatTrader(userAnswers = Some(importAnswers)).build()

        running(application) {
          val result = route(application, FakeRequest(POST, importRoute).withFormUrlEncodedBody(("value", ""))).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("vehicleIdentificationNumber.error.required"))
          contentAsString(result) must include("<strong>Volvo FH16</strong>")
        }
      }

      "must return a Bad Request and the duplicate error when another vehicle has the VIN, ignoring case" in {
        val answers = importAnswers.unsafeSet(
          AllVehiclesQuery,
          Map(
            "1" -> Json.obj(
              "importNumber" -> 1,
              "details"      -> Json.obj("bringingVehicleNI" -> true, "numberOfVehicles" -> Json.obj("oneMultiVehicle" -> "one"))
            ),
            "2" -> Json.obj("importNumber" -> 1, "details" -> Json.obj("vehicleIdentificationNumber" -> answer.toLowerCase))
          )
        )
        val (application, mockSessionRepository) = applicationWithMockRepository(applicationBuilderWithVatTrader(userAnswers = Some(answers)))

        running(application) {
          val result = route(application, FakeRequest(POST, importRoute).withFormUrlEncodedBody(("value", answer))).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("vehicleIdentificationNumber.error.duplicate"))
          verify(mockSessionRepository, never()).set(any())
        }
      }

      "must not treat the vehicle's own saved VIN as a duplicate" in {
        val answers = importAnswers.unsafeSet(
          AllVehiclesQuery,
          Map(
            "1" -> Json.obj(
              "importNumber" -> 1,
              "details"      -> Json.obj("vehicleIdentificationNumber" -> answer, "numberOfVehicles" -> Json.obj("oneMultiVehicle" -> "one"))
            )
          )
        )
        val (application, mockSessionRepository) = applicationWithMockRepository(applicationBuilderWithVatTrader(userAnswers = Some(answers)))

        running(application) {
          val result = route(application, FakeRequest(POST, importRoute).withFormUrlEncodedBody(("value", answer))).value

          status(result) mustEqual SEE_OTHER
          savedAnswers(mockSessionRepository).get(VehicleIdentificationNumberPage(vehicleNumber)) mustEqual Some(answer)
        }
      }

      "must redirect to Unauthorised for a GET if IQ1 was answered Yes" in {
        val answers = importAnswers.unsafeSet(VehicleFromEuPage, true)
        mustBeUnauthorised(applicationBuilderWithVatTrader(userAnswers = Some(answers)).build(), importRoute)
      }

      "must redirect to Unauthorised for a GET if draftId is missing" in {
        val answers = importAnswers.remove(DraftIdPage).success.value
        mustBeUnauthorised(applicationBuilderWithVatTrader(userAnswers = Some(answers)).build(), importRoute)
      }

      "must redirect to Unauthorised for a GET if the import number in the URL is not one of the user's imports" in {
        mustBeUnauthorised(
          applicationBuilderWithVatTrader(userAnswers = Some(importAnswers)).build(),
          vehicledetails.routes.VehicleIdentificationNumberController.importOnPageLoad(ImportNumber(2), vehicleNumber, NormalMode).url
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different import" in {
        val answers = importAnswers
          .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123-456789A"), "2" -> Json.obj("importEntryNumber" -> "x")))
          .unsafeSet(
            AllVehiclesQuery,
            Map(
              "1" -> Json.obj(
                "importNumber" -> 2,
                "details"      -> Json.obj("bringingVehicleNI" -> true, "numberOfVehicles" -> Json.obj("oneMultiVehicle" -> "one"))
              )
            )
          )

        mustBeUnauthorised(applicationBuilderWithVatTrader(userAnswers = Some(answers)).build(), importRoute)
      }
    }
  }
}
