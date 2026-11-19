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
import connectors.{NovaImportsBackendConnector, UpdateSectionError}
import controllers.actions.*
import controllers.{routes, vehicledetails}
import forms.DeleteVehicleFormProvider
import models.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{AddVehicleDetailsHeavyCommercialPage, AddVehicleTypePage, VehicleIdentificationNumberPage}
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import views.html.DeleteVehicleView

import scala.concurrent.Future

class DeleteVehicleControllerSpec extends SpecBase with MockitoSugar {

  val form = new DeleteVehicleFormProvider()()

  val supplierNumber = SupplierNumber(1)
  val importNumber   = ImportNumber(1)
  val vehicleNumber  = VehicleNumber(2)

  lazy val supplierRoute = vehicledetails.routes.DeleteVehicleController.supplierOnPageLoad(supplierNumber, vehicleNumber).url
  lazy val importRoute   = vehicledetails.routes.DeleteVehicleController.importOnPageLoad(importNumber, vehicleNumber).url

  lazy val supplierVehicles = vehicledetails.routes.VehiclesBoughtFromSupplierController.onPageLoad(supplierNumber).url
  lazy val importVehicles   = vehicledetails.routes.AddVehicleByImportEntryController.onPageLoad(importNumber).url

  private val vehicleDetails = Seq(
    "deleteVehicle.vin"   -> "1ABCD2E03FG456789",
    "deleteVehicle.make"  -> "Volvo",
    "deleteVehicle.model" -> "FH16"
  )

  private val otherVehicle = Json.obj("supplierNumber" -> 1, "details" -> Json.obj("vehicleIdentificationNumber" -> "9ZYXW8V76UT543210"))

  private def withVehicleDetails(answers: UserAnswers): UserAnswers =
    answers
      .unsafeSet(VehicleIdentificationNumberPage(vehicleNumber), "1ABCD2E03FG456789")
      .unsafeSet(AddVehicleTypePage(vehicleNumber), AddVehicleType.Hcv)
      .unsafeSet(
        AddVehicleDetailsHeavyCommercialPage(vehicleNumber),
        AddVehicleDetailsHeavyCommercial("Volvo", "FH16", "Tractor unit", "Sleeper cab")
      )

  val supplierAnswers: UserAnswers = withVehicleDetails(
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(DraftVersionIdPage, 3L)
      .unsafeSet(VehicleFromEuPage, true)
      .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
      .unsafeSet(AllVehiclesQuery, Map("1" -> otherVehicle, "2" -> Json.obj("supplierNumber" -> 1)))
  )

  val importAnswers: UserAnswers = withVehicleDetails(
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(DraftVersionIdPage, 3L)
      .unsafeSet(VehicleFromEuPage, false)
      .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123-456789A")))
      .unsafeSet(AllVehiclesQuery, Map("2" -> Json.obj("importNumber" -> 1)))
  )

  private def stubSessionRepository(answers: UserAnswers): SessionRepository = {
    val repository = mock[SessionRepository]
    when(repository.setPage(any(), any(), any())(any())).thenReturn(Future.successful(answers))
    repository
  }

  private def successfulConnector: NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.deleteDraftSections(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(Right(4L)))
    connector
  }

  private def application(
    identifierAction: Class[? <: IdentifierAction],
    answers: Option[UserAnswers],
    connector: NovaImportsBackendConnector = mock[NovaImportsBackendConnector],
    repository: SessionRepository = mock[SessionRepository]
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(answers)),
        bind[NovaImportsBackendConnector].toInstance(connector),
        bind[SessionRepository].toInstance(repository)
      )
      .build()

  private def mustBeUnauthorised(app: Application, url: String, method: String = GET): Unit =
    running(app) {
      val result = route(app, FakeRequest(method, url).withFormUrlEncodedBody(("value", "true"))).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }

  "DeleteVehicleController" - {

    "supplier journey" - {

      "must return OK and the correct view with the vehicle's VIN, make and model for a GET" in {
        val app = application(classOf[FakeIdentifierAction], Some(supplierAnswers))

        running(app) {
          val request = FakeRequest(GET, supplierRoute)
          val result  = route(app, request).value
          val view    = app.injector.instanceOf[DeleteVehicleView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            vehicleDetails,
            vehicledetails.routes.DeleteVehicleController.supplierOnSubmit(supplierNumber, vehicleNumber)
          )(request, messages(app)).toString
        }
      }

      "must return a Bad Request and an error message when nothing is selected" in {
        val app = application(classOf[FakeIdentifierAction], Some(supplierAnswers))

        running(app) {
          val result = route(app, FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", ""))).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include("Select yes if you want to delete this vehicle")
        }
      }

      "must go back to the supplier's vehicles without deleting anything when No is selected" in {
        val connector  = mock[NovaImportsBackendConnector]
        val repository = mock[SessionRepository]
        val app        = application(classOf[FakeIdentifierAction], Some(supplierAnswers), connector, repository)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", "false"))).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual supplierVehicles
          verify(connector, never()).deleteDraftSections(any(), any(), any())(any[HeaderCarrier])
          verify(repository, never()).setPage(any(), any(), any())(any())
        }
      }

      "must delete the vehicle from FormP and the session and go back to the supplier's vehicles when Yes is selected" in {
        val connector  = successfulConnector
        val repository = stubSessionRepository(supplierAnswers)
        val app        = application(classOf[FakeIdentifierAction], Some(supplierAnswers), connector, repository)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", "true"))).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual supplierVehicles
          verify(connector).deleteDraftSections(
            eqTo(DraftId("DRAFT-001")),
            eqTo(Seq("supplier/1/vehicle/2/type", "supplier/1/vehicle/2/details", "supplier/1/vehicle/2/additional-information")),
            eqTo(3L)
          )(any[HeaderCarrier])
          verify(repository).setPage(any(), eqTo(DraftVersionIdPage), eqTo(4L))(any())
          verify(repository).setPage(any(), eqTo(AllVehiclesQuery), eqTo(Map("1" -> otherVehicle, "2" -> Json.obj("deleted" -> true))))(any())
        }
      }

      "must redirect to There is a problem and keep the vehicle when the FormP delete fails" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.deleteDraftSections(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(502, "error"))))
        val repository = mock[SessionRepository]
        val app        = application(classOf[FakeIdentifierAction], Some(supplierAnswers), connector, repository)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", "true"))).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(repository, never()).setPage(any(), any(), any())(any())
        }
      }

      "must redirect to Unauthorised and keep the vehicle when the draft belongs to another user" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.deleteDraftSections(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(Left(UpdateSectionError.Forbidden)))
        val repository = mock[SessionRepository]
        val app        = application(classOf[FakeIdentifierAction], Some(supplierAnswers), connector, repository)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", "true"))).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
          verify(repository, never()).setPage(any(), any(), any())(any())
        }
      }

      "must redirect to There is a problem without calling FormP when the version id is missing" in {
        val connector = mock[NovaImportsBackendConnector]
        val app       = application(classOf[FakeIdentifierAction], Some(supplierAnswers.remove(DraftVersionIdPage).success.value), connector)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", "true"))).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(connector, never()).deleteDraftSections(any(), any(), any())(any[HeaderCarrier])
        }
      }

      "must return OK for a GET from a non-VAT organisation, a VAT organisation and an agent" in {
        Seq(classOf[FakeOrganisationIdentifierAction], classOf[FakeVatTraderIdentifierAction], classOf[FakeAgentIdentifierAction]).foreach {
          identifierAction =>
            val app = application(identifierAction, Some(supplierAnswers))

            running(app) {
              status(route(app, FakeRequest(GET, supplierRoute)).value) mustEqual OK
            }
        }
      }

      "must redirect to Unauthorised if no session data is found" in {
        mustBeUnauthorised(application(classOf[FakeIdentifierAction], None), supplierRoute)
      }

      "must redirect to Unauthorised for a GET and a POST if no draft is in progress" in {
        val answers = supplierAnswers.remove(DraftIdPage).success.value

        mustBeUnauthorised(application(classOf[FakeIdentifierAction], Some(answers)), supplierRoute)
        mustBeUnauthorised(application(classOf[FakeIdentifierAction], Some(answers)), supplierRoute, POST)
      }

      "must redirect to Unauthorised if the vehicle was not brought from the EU" in {
        mustBeUnauthorised(application(classOf[FakeIdentifierAction], Some(supplierAnswers.unsafeSet(VehicleFromEuPage, false))), supplierRoute)
      }

      "must redirect to Unauthorised if the supplier number in the URL is not one of the user's suppliers" in {
        mustBeUnauthorised(
          application(classOf[FakeIdentifierAction], Some(supplierAnswers)),
          vehicledetails.routes.DeleteVehicleController.supplierOnPageLoad(SupplierNumber(2), vehicleNumber).url
        )
      }

      "must redirect to Unauthorised if the vehicle in the URL belongs to a different supplier" in {
        val answers = supplierAnswers
          .unsafeSet(
            AllSuppliersQuery,
            Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false), "2" -> Json.obj("usePersonalDetailsAsSupplier" -> false))
          )
          .unsafeSet(
            AllVehiclesQuery,
            Map(
              "1" -> otherVehicle,
              "2" -> Json.obj("supplierNumber" -> 2, "details" -> Json.obj("vehicleIdentificationNumber" -> "1ABCD2E03FG456789"))
            )
          )

        mustBeUnauthorised(application(classOf[FakeIdentifierAction], Some(answers)), supplierRoute)
      }

      "must redirect to Unauthorised if the vehicle in the URL is not in the session" in {
        mustBeUnauthorised(
          application(classOf[FakeIdentifierAction], Some(supplierAnswers)),
          vehicledetails.routes.DeleteVehicleController.supplierOnPageLoad(supplierNumber, VehicleNumber(3)).url
        )
      }

      "must redirect to Unauthorised if the vehicle has already been deleted" in {
        val answers = supplierAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> otherVehicle, "2" -> Json.obj("deleted" -> true)))

        mustBeUnauthorised(application(classOf[FakeIdentifierAction], Some(answers)), supplierRoute, POST)
      }
    }

    "import journey" - {

      "must return OK and the correct view with the vehicle's VIN, make and model for a GET" in {
        val app = application(classOf[FakeVatTraderIdentifierAction], Some(importAnswers))

        running(app) {
          val request = FakeRequest(GET, importRoute)
          val result  = route(app, request).value
          val view    = app.injector.instanceOf[DeleteVehicleView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            vehicleDetails,
            vehicledetails.routes.DeleteVehicleController.importOnSubmit(importNumber, vehicleNumber)
          )(request, messages(app)).toString
        }
      }

      "must go back to the import entry's vehicles without deleting anything when No is selected" in {
        val connector = mock[NovaImportsBackendConnector]
        val app       = application(classOf[FakeVatTraderIdentifierAction], Some(importAnswers), connector)

        running(app) {
          val result = route(app, FakeRequest(POST, importRoute).withFormUrlEncodedBody(("value", "false"))).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual importVehicles
          verify(connector, never()).deleteDraftSections(any(), any(), any())(any[HeaderCarrier])
        }
      }

      "must delete the vehicle from FormP and the session and go back to the import entry's vehicles when Yes is selected" in {
        val connector  = successfulConnector
        val repository = stubSessionRepository(importAnswers)
        val app        = application(classOf[FakeVatTraderIdentifierAction], Some(importAnswers), connector, repository)

        running(app) {
          val result = route(app, FakeRequest(POST, importRoute).withFormUrlEncodedBody(("value", "true"))).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual importVehicles
          verify(connector).deleteDraftSections(
            eqTo(DraftId("DRAFT-001")),
            eqTo(Seq("import/1/vehicle/2/type", "import/1/vehicle/2/details", "import/1/vehicle/2/additional-information")),
            eqTo(3L)
          )(any[HeaderCarrier])
          verify(repository).setPage(any(), eqTo(DraftVersionIdPage), eqTo(4L))(any())
          verify(repository).setPage(any(), eqTo(AllVehiclesQuery), eqTo(Map("2" -> Json.obj("deleted" -> true))))(any())
        }
      }

      "must return OK for a GET from an agent" in {
        val app = application(classOf[FakeAgentIdentifierAction], Some(importAnswers))

        running(app) {
          status(route(app, FakeRequest(GET, importRoute)).value) mustEqual OK
        }
      }

      "must redirect to Unauthorised for a private individual and a non-VAT organisation" in {
        mustBeUnauthorised(application(classOf[FakeIdentifierAction], Some(importAnswers)), importRoute)
        mustBeUnauthorised(application(classOf[FakeOrganisationIdentifierAction], Some(importAnswers)), importRoute)
      }

      "must redirect to Unauthorised for a GET and a POST if no draft is in progress" in {
        val answers = importAnswers.remove(DraftIdPage).success.value

        mustBeUnauthorised(application(classOf[FakeVatTraderIdentifierAction], Some(answers)), importRoute)
        mustBeUnauthorised(application(classOf[FakeVatTraderIdentifierAction], Some(answers)), importRoute, POST)
      }

      "must redirect to Unauthorised if the vehicle was brought from the EU" in {
        mustBeUnauthorised(application(classOf[FakeVatTraderIdentifierAction], Some(importAnswers.unsafeSet(VehicleFromEuPage, true))), importRoute)
      }

      "must redirect to Unauthorised if the vehicle in the URL belongs to a different import" in {
        val answers = importAnswers
          .unsafeSet(
            AllImportsQuery,
            Map("1" -> Json.obj("importEntryNumber" -> "123-456789A"), "2" -> Json.obj("importEntryNumber" -> "987-654321B"))
          )
          .unsafeSet(
            AllVehiclesQuery,
            Map("2" -> Json.obj("importNumber" -> 2, "details" -> Json.obj("vehicleIdentificationNumber" -> "1ABCD2E03FG456789")))
          )

        mustBeUnauthorised(application(classOf[FakeVatTraderIdentifierAction], Some(answers)), importRoute)
      }
    }
  }
}
