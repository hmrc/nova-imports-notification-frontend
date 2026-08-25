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
import config.FrontendAppConfig
import connectors.{GetTraderInformationError, NovaImportsBackendConnector}
import controllers.{routes, vehicledetails}
import models.{BusinessOrPrivateIndividual, DraftId, NameDetails, NormalMode, SupplierNumber, TraderInformation, UserAnswers, VehicleNumber}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.{BusinessOrPrivatePage, VehicleBusinessUsePage, VehicleFromEuPage}
import pages.sections.notifierdetails.{BusinessNamePage, NameDetailsPage}
import pages.sections.supplierdetails.{SupplierBusinessNamePage, SupplierBusinessOrIndividualPage, SupplierNamePage, UsePersonalDetailsAsSupplierPage}
import play.api.inject.bind
import play.api.libs.json.Json
import queries.{AllSuppliersQuery, AllVehiclesQuery}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.VehiclesBoughtFromSupplierView

import scala.concurrent.Future

class VehiclesBoughtFromSupplierControllerSpec extends SpecBase with MockitoSugar {

  val supplierNumber: SupplierNumber = SupplierNumber(1)

  val userAnswersWithGuardData: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj()))

  lazy val vehiclesBoughtFromSupplierRoute: String =
    vehicledetails.routes.VehiclesBoughtFromSupplierController.onPageLoad(supplierNumber).url

  private val traderInformation: TraderInformation = TraderInformation(
    traderName = Some("Acme Trading Ltd"),
    tradingName = Some("Acme Trading"),
    addressLine1 = Some("1 High Street"),
    addressLine2 = Some("Testtown"),
    addressLine3 = None,
    addressLine4 = None,
    postcode = Some("TF3 4ER")
  )

  private def mockSessionRepository(userAnswers: UserAnswers): SessionRepository = {
    val repo = mock[SessionRepository]
    when(repo.setPage(any(), any(), any())(any())) thenReturn Future.successful(userAnswers)
    repo
  }

  private def applicationWith(userAnswers: UserAnswers, repo: SessionRepository): play.api.Application =
    applicationBuilder(userAnswers = Some(userAnswers))
      .overrides(bind[SessionRepository].toInstance(repo))
      .build()

  private def applicationWithMockRepository(userAnswers: UserAnswers): play.api.Application =
    applicationWith(userAnswers, mockSessionRepository(userAnswers))

  private def connectorReturning(result: Either[GetTraderInformationError, TraderInformation]): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getTraderInformation()(any())) thenReturn Future.successful(result)
    connector
  }

  private val vatTraderAnswersUsingPersonalDetails: UserAnswers =
    userAnswersWithGuardData.unsafeSet(UsePersonalDetailsAsSupplierPage(supplierNumber), true)

  "VehiclesBoughtFromSupplierController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, vehiclesBoughtFromSupplierRoute)

        val result = route(application, request).value

        val view      = application.injector.instanceOf[VehiclesBoughtFromSupplierView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(None, supplierNumber, appConfig.personalTransportUnitUrl)(request, messages(application)).toString
      }
    }

    "must add vehicle 1 for the supplier and send the user to AVD3.0 for that vehicle" in {

      val application = applicationWithMockRepository(userAnswersWithGuardData)

      running(application) {
        val result = route(application, FakeRequest(POST, vehiclesBoughtFromSupplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          vehicledetails.routes.VehicleDatesController.onPageLoad(supplierNumber, VehicleNumber(1), NormalMode).url
      }
    }

    "must add the next vehicle when the notification already has one" in {

      val answers     = userAnswersWithGuardData.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
      val application = applicationWithMockRepository(answers)

      running(application) {
        val result = route(application, FakeRequest(POST, vehiclesBoughtFromSupplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          vehicledetails.routes.VehicleDatesController.onPageLoad(supplierNumber, VehicleNumber(2), NormalMode).url
      }
    }

    "must pass the supplier number from URL to addForSupplier so the vehicle is saved with supplierNumber 1" in {

      val repo        = mockSessionRepository(userAnswersWithGuardData)
      val application = applicationWith(userAnswersWithGuardData, repo)

      running(application) {
        route(application, FakeRequest(POST, vehiclesBoughtFromSupplierRoute)).value.futureValue

        verify(repo).setPage(any(), eqTo(AllVehiclesQuery), eqTo(Map("1" -> Json.obj("supplierNumber" -> 1))))(any())
      }
    }

    "must redirect to Unauthorised for a POST if there is no draft id in session" in {

      val answers = emptyUserAnswers
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj()))

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(POST, vehiclesBoughtFromSupplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if there is no draft id in session" in {

      val answers = emptyUserAnswers
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj()))

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the vehicle was not brought from the EU" in {

      val answers = userAnswersWithGuardData.unsafeSet(VehicleFromEuPage, false)

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val otherSupplierRoute = vehicledetails.routes.VehiclesBoughtFromSupplierController.onPageLoad(SupplierNumber(2)).url

        val result = route(application, FakeRequest(GET, otherSupplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must render the supplier business name in the heading" in {

      val answers = userAnswersWithGuardData
        .unsafeSet(UsePersonalDetailsAsSupplierPage(supplierNumber), false)
        .unsafeSet(SupplierBusinessOrIndividualPage(supplierNumber), BusinessOrPrivateIndividual.Business)
        .unsafeSet(SupplierBusinessNamePage(supplierNumber), "ABC Ltd")

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

        contentAsString(result) must include("Vehicles bought from ABC Ltd")
      }
    }

    "must render the supplier's own name in the heading" in {

      val answers = userAnswersWithGuardData
        .unsafeSet(UsePersonalDetailsAsSupplierPage(supplierNumber), false)
        .unsafeSet(SupplierBusinessOrIndividualPage(supplierNumber), BusinessOrPrivateIndividual.PrivateIndividual)
        .unsafeSet(SupplierNamePage(supplierNumber), NameDetails("Mr", "John", "Smith"))

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

        contentAsString(result) must include("Vehicles bought from Mr John Smith")
      }
    }

    "must render the notifier's business name in the heading when they use their own details as the supplier for a business" in {

      val answers = userAnswersWithGuardData
        .unsafeSet(UsePersonalDetailsAsSupplierPage(supplierNumber), true)
        .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
        .unsafeSet(BusinessNamePage, "Acme Trading Co Ltd")

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

        contentAsString(result) must include("Vehicles bought from Acme Trading Co Ltd")
      }
    }

    "must render the notifier's own name in the heading when they use their own details as the supplier as a private individual" in {

      val answers = userAnswersWithGuardData
        .unsafeSet(UsePersonalDetailsAsSupplierPage(supplierNumber), true)
        .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
        .unsafeSet(NameDetailsPage, NameDetails("Mr", "John", "Smith"))

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

        contentAsString(result) must include("Vehicles bought from Mr John Smith")
      }
    }

    "for a VAT-registered organisation using their own details as the supplier" - {

      "must render the trader name from the RDS record when the vehicle is for business use" in {

        val answers   = vatTraderAnswersUsingPersonalDetails.unsafeSet(VehicleBusinessUsePage, true)
        val connector = connectorReturning(Right(traderInformation))

        val application = applicationBuilderWithVatTrader(userAnswers = Some(answers))
          .overrides(bind[NovaImportsBackendConnector].toInstance(connector))
          .build()

        running(application) {
          val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

          status(result) mustEqual OK
          contentAsString(result) must include("Vehicles bought from Acme Trading Ltd")
        }
      }

      "must render the fallback heading when the trader lookup finds no record" in {

        val answers   = vatTraderAnswersUsingPersonalDetails.unsafeSet(VehicleBusinessUsePage, true)
        val connector = connectorReturning(Left(GetTraderInformationError.NotFound))

        val application = applicationBuilderWithVatTrader(userAnswers = Some(answers))
          .overrides(bind[NovaImportsBackendConnector].toInstance(connector))
          .build()

        running(application) {
          val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

          status(result) mustEqual OK
          contentAsString(result) must include("""<h1 class="govuk-heading-l">Vehicles bought from this supplier</h1>""")
        }
      }

      "must render the fallback heading when the trader lookup throws" in {

        val answers   = vatTraderAnswersUsingPersonalDetails.unsafeSet(VehicleBusinessUsePage, true)
        val connector = mock[NovaImportsBackendConnector]
        when(connector.getTraderInformation()(any())) thenReturn Future.failed(new RuntimeException("connection reset"))

        val application = applicationBuilderWithVatTrader(userAnswers = Some(answers))
          .overrides(bind[NovaImportsBackendConnector].toInstance(connector))
          .build()

        running(application) {
          val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

          status(result) mustEqual OK
          contentAsString(result) must include("""<h1 class="govuk-heading-l">Vehicles bought from this supplier</h1>""")
        }
      }

      "must render the name from the notifier's details when the vehicle is not for business use" in {

        val answers = vatTraderAnswersUsingPersonalDetails
          .unsafeSet(VehicleBusinessUsePage, false)
          .unsafeSet(NameDetailsPage, NameDetails("Mr", "John", "Smith"))

        val connector = connectorReturning(Right(traderInformation))

        val application = applicationBuilderWithVatTrader(userAnswers = Some(answers))
          .overrides(bind[NovaImportsBackendConnector].toInstance(connector))
          .build()

        running(application) {
          val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

          contentAsString(result) must include("Vehicles bought from Mr John Smith")
          verify(connector, never).getTraderInformation()(any())
        }
      }
    }
  }
}
