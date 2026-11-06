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
import controllers.{routes, vehicledetails}
import models.{AddVehicleDetailsCar, AddVehicleType, DraftId, ImportNumber, NormalMode, NumberOfVehicles, OneMultiVehicle, SupplierNumber, UserAnswers, VehicleNumber}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.*
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import views.html.NumberOfVehiclesView

import scala.concurrent.Future

class NumberOfVehiclesControllerSpec extends SpecBase with MockitoSugar {

  private val s = SupplierNumber(1)
  private val i = ImportNumber(1)
  private val v = VehicleNumber(1)

  private lazy val supplierRoute = vehicledetails.routes.NumberOfVehiclesController.supplierOnPageLoad(s, v, NormalMode).url
  private lazy val importRoute   = vehicledetails.routes.NumberOfVehiclesController.importOnPageLoad(i, v, NormalMode).url

  private val carDetails = AddVehicleDetailsCar("Land Rover", "Discovery 4", "3.0TD", "XS", "station wagon")

  private val supplierBase: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(DraftVersionIdPage, 3L)
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
    .unsafeSet(AddVehicleTypePage(v), AddVehicleType.Car)
    .unsafeSet(AddVehicleDetailsCarPage(v), carDetails)

  private val importBase: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(DraftVersionIdPage, 3L)
    .unsafeSet(VehicleFromEuPage, false)
    .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A")))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1)))
    .unsafeSet(AddVehicleTypePage(v), AddVehicleType.Car)
    .unsafeSet(AddVehicleDetailsCarPage(v), carDetails)

  private def stubSessionRepository: SessionRepository = {
    val repository = mock[SessionRepository]
    when(repository.set(any())).thenReturn(Future.successful(true))
    repository
  }

  private def application(answers: Option[UserAnswers], repository: SessionRepository = stubSessionRepository): Application =
    applicationBuilder(answers)
      .overrides(bind[SessionRepository].toInstance(repository))
      .build()

  private def mustBeUnauthorised(app: Application, url: String, method: String = GET): Unit =
    running(app) {
      val result = route(app, FakeRequest(method, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }

  "NumberOfVehiclesController" - {

    "supplierOnPageLoad" - {

      "must return OK with the empty form and the vehicle details inset" in {
        val app = application(Some(supplierBase))

        running(app) {
          implicit val msgs: Messages = messages(app)

          val request = FakeRequest(GET, supplierRoute)
          val result  = route(app, request).value
          val view    = app.injector.instanceOf[NumberOfVehiclesView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            new forms.NumberOfVehiclesFormProvider()(),
            viewmodels.VehicleDetailsInset(supplierBase, v),
            vehicledetails.routes.NumberOfVehiclesController.supplierOnSubmit(s, v, NormalMode)
          )(request, msgs).toString
          contentAsString(result) must include("Land Rover")
        }
      }

      "must pre-fill the form when an answer has already been saved" in {
        val answers = supplierBase.unsafeSet(NumberOfVehiclesPage(v), NumberOfVehicles(OneMultiVehicle.More, Some(5)))
        val app     = application(Some(answers))

        running(app) {
          val result = route(app, FakeRequest(GET, supplierRoute)).value

          status(result) mustEqual OK
          contentAsString(result) must include("""value="5"""")
        }
      }

      "must redirect to Unauthorised when draftId is missing" in {
        mustBeUnauthorised(application(Some(supplierBase.remove(DraftIdPage).success.value)), supplierRoute)
      }

      "must redirect to Unauthorised when the vehicle was not brought from the EU" in {
        mustBeUnauthorised(application(Some(supplierBase.unsafeSet(VehicleFromEuPage, false))), supplierRoute)
      }

      "must redirect to Unauthorised when the vehicle does not belong to the supplier" in {
        mustBeUnauthorised(
          application(Some(supplierBase)),
          vehicledetails.routes.NumberOfVehiclesController.supplierOnPageLoad(SupplierNumber(2), v, NormalMode).url
        )
      }

      "must redirect to Unauthorised when the vehicle type details are missing" in {
        mustBeUnauthorised(application(Some(supplierBase.remove(AddVehicleDetailsCarPage(v)).success.value)), supplierRoute)
      }
    }

    "importOnPageLoad" - {

      "must return OK and post to the import submit" in {
        val app = application(Some(importBase))

        running(app) {
          val result = route(app, FakeRequest(GET, importRoute)).value

          status(result) mustEqual OK
          contentAsString(result) must include(
            s"""action="${vehicledetails.routes.NumberOfVehiclesController.importOnSubmit(i, v, NormalMode).url}""""
          )
        }
      }

      "must redirect to Unauthorised when the vehicle was brought from the EU" in {
        mustBeUnauthorised(application(Some(importBase.unsafeSet(VehicleFromEuPage, true))), importRoute)
      }

      "must redirect to Unauthorised when the vehicle does not belong to the import" in {
        mustBeUnauthorised(
          application(Some(importBase)),
          vehicledetails.routes.NumberOfVehiclesController.importOnPageLoad(ImportNumber(2), v, NormalMode).url
        )
      }
    }

    "supplierOnSubmit" - {

      "must save 'one vehicle' to the session and redirect, without calling the backend" in {
        val repository = stubSessionRepository
        val app        = application(Some(supplierBase), repository)

        running(app) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody("oneMultiVehicle" -> "one")
          val result  = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.LandingPageController.onPageLoad().url

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(repository).set(captor.capture())
          captor.getValue.get(NumberOfVehiclesPage(v)) mustEqual Some(NumberOfVehicles(OneMultiVehicle.One, None))
        }
      }

      "must save 'more than one vehicle' with the given count to the session and redirect" in {
        val repository = stubSessionRepository
        val app        = application(Some(supplierBase), repository)

        running(app) {
          val request =
            FakeRequest(POST, supplierRoute).withFormUrlEncodedBody("oneMultiVehicle" -> "more", "numberSameVehicles" -> "5")
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual vehicledetails.routes.VehicleIdentificationNumbersController.supplierOnPageLoad(s, v).url

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(repository).set(captor.capture())
          captor.getValue.get(NumberOfVehiclesPage(v)) mustEqual Some(NumberOfVehicles(OneMultiVehicle.More, Some(5)))
        }
      }

      "must redirect without saving when 'more than one' is selected but another vehicle already has values" in {
        val repository       = stubSessionRepository
        val existingVehicles = supplierBase.get(AllVehiclesQuery).getOrElse(Map.empty)
        val answers          = supplierBase.unsafeSet(
          AllVehiclesQuery,
          existingVehicles + ("2" -> Json.obj("supplierNumber" -> 1, "make" -> "Ford"))
        )
        val app = application(Some(answers), repository)

        running(app) {
          val request =
            FakeRequest(POST, supplierRoute).withFormUrlEncodedBody("oneMultiVehicle" -> "more", "numberSameVehicles" -> "5")
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.LandingPageController.onPageLoad().url
          verify(repository, never).set(any())
        }
      }

      "must return BAD_REQUEST when no option is selected" in {
        val app = application(Some(supplierBase))

        running(app) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody()
          val result  = route(app, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(app)("numberOfVehicles.error.required.selection"))
        }
      }

      "must return BAD_REQUEST when 'more than one' is selected with no count given" in {
        val app = application(Some(supplierBase))

        running(app) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody("oneMultiVehicle" -> "more")
          val result  = route(app, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(app)("numberOfVehicles.error.required.count"))
        }
      }

      "must return BAD_REQUEST when the count is out of range" in {
        val app = application(Some(supplierBase))

        running(app) {
          val request =
            FakeRequest(POST, supplierRoute).withFormUrlEncodedBody("oneMultiVehicle" -> "more", "numberSameVehicles" -> "101")
          val result = route(app, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(app)("numberOfVehicles.error.length"))
        }
      }

      "must redirect to Unauthorised when the vehicle does not belong to the supplier" in {
        val app = application(Some(supplierBase))

        running(app) {
          val request =
            FakeRequest(POST, vehicledetails.routes.NumberOfVehiclesController.supplierOnSubmit(SupplierNumber(2), v, NormalMode).url)
              .withFormUrlEncodedBody("oneMultiVehicle" -> "one")
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "importOnSubmit" - {

      "must save the answer to the session and redirect" in {
        val repository = stubSessionRepository
        val app        = application(Some(importBase), repository)

        running(app) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody("oneMultiVehicle" -> "one")
          val result  = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.LandingPageController.onPageLoad().url

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(repository).set(captor.capture())
          captor.getValue.get(NumberOfVehiclesPage(v)) mustEqual Some(NumberOfVehicles(OneMultiVehicle.One, None))
        }
      }

      "must return BAD_REQUEST when no option is selected" in {
        val app = application(Some(importBase))

        running(app) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody()
          val result  = route(app, request).value

          status(result) mustEqual BAD_REQUEST
        }
      }
    }
  }
}
