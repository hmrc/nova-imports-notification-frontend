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
import forms.MileageFormProvider
import models.*
import models.MileageUnit.MILES
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{AddVehicleDetailsHeavyCommercialPage, AddVehicleTypePage, MileagePage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import viewmodels.VehicleDetailsInset
import views.html.MileageView

import scala.concurrent.Future

class MileageControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val form = new MileageFormProvider()()

  val supplierNumber = SupplierNumber(1)
  val importNumber   = ImportNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  val answer = Mileage(MILES, "1234")

  lazy val supplierRoute = vehicledetails.routes.MileageController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode).url
  lazy val importRoute   = vehicledetails.routes.MileageController.importOnPageLoad(importNumber, vehicleNumber, NormalMode).url

  lazy val supplierSubmit = vehicledetails.routes.MileageController.supplierOnSubmit(supplierNumber, vehicleNumber, NormalMode)
  lazy val importSubmit   = vehicledetails.routes.MileageController.importOnSubmit(importNumber, vehicleNumber, NormalMode)

  private def withVehicleDetails(answers: UserAnswers): UserAnswers =
    answers
      .unsafeSet(AddVehicleTypePage(vehicleNumber), AddVehicleType.Hcv)
      .unsafeSet(
        AddVehicleDetailsHeavyCommercialPage(vehicleNumber),
        AddVehicleDetailsHeavyCommercial("Volvo", "FH16", "Tractor unit", "Sleeper cab")
      )

  val supplierAnswers: UserAnswers = withVehicleDetails(
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(VehicleFromEuPage, true)
      .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
      .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))))
  )

  val importAnswers: UserAnswers = withVehicleDetails(
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(VehicleFromEuPage, false)
      .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123-456789A")))
      .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))))
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

  "MileageController" - {

    "supplier journey" - {

      "must return OK and the correct view with the vehicle details for a GET" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[MileageView]
          val msgs    = messages(application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            VehicleDetailsInset(supplierAnswers, vehicleNumber)(msgs),
            supplierSubmit
          )(request, msgs).toString
          contentAsString(result) must include("<b>Volvo FH16</b>")
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = supplierAnswers.unsafeSet(MileagePage(vehicleNumber), answer)
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[MileageView]
          val msgs    = messages(application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(answer),
            VehicleDetailsInset(userAnswers, vehicleNumber)(msgs),
            supplierSubmit
          )(request, msgs).toString
        }
      }

      "must redirect to the next page and save the answer when valid data is submitted" in {
        val (application, mockSessionRepository) = applicationWithMockRepository(applicationBuilder(userAnswers = Some(supplierAnswers)))

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("unit", answer.mileageUnits.toString), ("value", answer.mileage))
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(MileagePage(vehicleNumber)) mustEqual Some(answer)
        }
      }

      "must return a Bad Request with the vehicle details and the required error when no mileage value is provided" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("unit", answer.mileageUnits.toString), ("value", ""))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("mileage.error.required.mileage"))
          contentAsString(result) must include("<b>Volvo FH16</b>")
        }
      }

      "must return a Bad Request with the vehicle details and the required error when no mileage unit is provided" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("unit", ""), ("value", answer.mileage))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("mileage.error.required.unit"))
          contentAsString(result) must include("<b>Volvo FH16</b>")
        }
      }

      "must return a Bad Request and the format error when the milage is in the wrong format" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("unit", answer.mileageUnits.toString), ("value", "inval"))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("mileage.error.format"))
        }
      }

      "must return a Bad Request and the length error when the mileage is over 6 characters" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", "1234567"))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("mileage.error.length"))
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

      "must redirect to Unauthorised for a GET if IQ1 was answered No" in {
        val answers = supplierAnswers.unsafeSet(VehicleFromEuPage, false)
        mustBeUnauthorised(applicationBuilder(userAnswers = Some(answers)).build(), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {
        mustBeUnauthorised(
          applicationBuilder(userAnswers = Some(supplierAnswers)).build(),
          vehicledetails.routes.MileageController.supplierOnPageLoad(SupplierNumber(2), vehicleNumber, NormalMode).url
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different supplier" in {
        val answers = supplierAnswers
          .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj(), "2" -> Json.obj()))
          .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 2)))

        mustBeUnauthorised(applicationBuilder(userAnswers = Some(answers)).build(), supplierRoute)
      }
    }

    "import journey" - {

      "must return OK and the correct view with the vehicle details for a GET" in {
        val application = applicationBuilderWithVatTrader(userAnswers = Some(importAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, importRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[MileageView]
          val msgs    = messages(application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            VehicleDetailsInset(importAnswers, vehicleNumber)(msgs),
            importSubmit
          )(request, msgs).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = importAnswers.unsafeSet(MileagePage(vehicleNumber), answer)
        val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, importRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[MileageView]
          val msgs    = messages(application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(answer),
            VehicleDetailsInset(userAnswers, vehicleNumber)(msgs),
            importSubmit
          )(request, msgs).toString
        }
      }

      "must redirect to the next page and save the answer when valid data is submitted" in {
        val (application, mockSessionRepository) =
          applicationWithMockRepository(applicationBuilderWithVatTrader(userAnswers = Some(importAnswers)))

        running(application) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody(("unit", answer.mileageUnits.toString), ("value", answer.mileage))
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(MileagePage(vehicleNumber)) mustEqual Some(answer)
        }
      }

      "must return a Bad Request and the required error when mileage unit is not entered" in {
        val application = applicationBuilderWithVatTrader(userAnswers = Some(importAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody(("unit", ""), ("value", answer.mileage))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("mileage.error.required.unit"))
        }
      }

      "must return a Bad Request and the required error when mileage value is not entered" in {
        val application = applicationBuilderWithVatTrader(userAnswers = Some(importAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody(("unit", answer.mileageUnits.toString), ("value", ""))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("mileage.error.required.mileage"))
        }
      }

      "must return OK for a GET for an agent" in {
        val application = applicationForUserType(classOf[FakeAgentIdentifierAction], importAnswers)

        running(application) {
          status(route(application, FakeRequest(GET, importRoute)).value) mustEqual OK
        }
      }

      "must redirect to Unauthorised for a private individual (user type 1)" in {
        mustBeUnauthorised(applicationForUserType(classOf[FakeIdentifierAction], importAnswers), importRoute)
      }

      "must redirect to Unauthorised for a non-VAT organisation (user type 2)" in {
        mustBeUnauthorised(applicationForUserType(classOf[FakeOrganisationIdentifierAction], importAnswers), importRoute)
      }

      "must redirect to Unauthorised for a GET if draftId is missing" in {
        val answers = importAnswers.remove(DraftIdPage).success.value
        mustBeUnauthorised(applicationBuilderWithVatTrader(userAnswers = Some(answers)).build(), importRoute)
      }

      "must redirect to Unauthorised for a GET if IQ1 was answered Yes" in {
        val answers = importAnswers.unsafeSet(VehicleFromEuPage, true)
        mustBeUnauthorised(applicationBuilderWithVatTrader(userAnswers = Some(answers)).build(), importRoute)
      }

      "must redirect to Unauthorised for a GET if the import number in the URL is not one of the user's imports" in {
        mustBeUnauthorised(
          applicationBuilderWithVatTrader(userAnswers = Some(importAnswers)).build(),
          vehicledetails.routes.MileageController.importOnPageLoad(ImportNumber(2), vehicleNumber, NormalMode).url
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different import" in {
        val answers = importAnswers
          .unsafeSet(
            AllImportsQuery,
            Map("1" -> Json.obj("importEntryNumber" -> "123-456789A"), "2" -> Json.obj("importEntryNumber" -> "987-654321B"))
          )
          .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 2, "details" -> Json.obj("bringingVehicleNI" -> true))))

        mustBeUnauthorised(applicationBuilderWithVatTrader(userAnswers = Some(answers)).build(), importRoute)
      }
    }
  }
}
