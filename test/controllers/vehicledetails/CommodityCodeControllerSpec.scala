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
import config.FrontendAppConfig
import controllers.actions.*
import controllers.{routes, vehicledetails}
import forms.CommodityCodeFormProvider
import models.{AddVehicleDetailsHeavyCommercial, AddVehicleType, CheckMode, DraftId, ImportNumber, Mode, NormalMode, UserAnswers, VehicleNumber}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{AddVehicleDetailsHeavyCommercialPage, AddVehicleTypePage, CommodityCodePage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllVehiclesQuery}
import repositories.SessionRepository
import viewmodels.VehicleDetailsInset
import views.html.CommodityCodeView

import scala.concurrent.Future

class CommodityCodeControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val form = new CommodityCodeFormProvider()()

  val importNumber  = ImportNumber(1)
  val vehicleNumber = VehicleNumber(1)

  lazy val pageRoute = vehicledetails.routes.CommodityCodeController.onPageLoad(importNumber, vehicleNumber, NormalMode).url

  val importJourneyAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, false)
    .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A")))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))))

  private def submitCall(mode: Mode): Call =
    vehicledetails.routes.CommodityCodeController.onSubmit(importNumber, vehicleNumber, mode)

  private def expectedView(application: Application, form: play.api.data.Form[String], answers: UserAnswers, mode: Mode)(
    request: Request[?]
  ): String = {
    val view      = application.injector.instanceOf[CommodityCodeView]
    val appConfig = application.injector.instanceOf[FrontendAppConfig]
    val msgs      = messages(application)
    view(form, VehicleDetailsInset(answers, vehicleNumber)(msgs), submitCall(mode), appConfig.findingCommodityCodesUrl)(request, msgs).toString
  }

  private def applicationFor(identifierAction: Class[? <: IdentifierAction], userAnswers: Option[UserAnswers]): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )
      .build()

  private def applicationWithMockRepository(userAnswers: UserAnswers): (Application, SessionRepository) = {

    val mockSessionRepository = mock[SessionRepository]
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

    val application =
      applicationBuilderWithVatTrader(userAnswers = Some(userAnswers))
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

  private def assertUnauthorisedOnGet(application: Application, url: String = pageRoute) =
    running(application) {
      val result = route(application, FakeRequest(GET, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }

  private def assertUnauthorisedOnPost(application: Application, url: String = pageRoute) =
    running(application) {
      val result = route(application, FakeRequest(POST, url).withFormUrlEncodedBody("value" -> "8703123456")).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }

  private def vatTrader(userAnswers: Option[UserAnswers]): Application =
    applicationBuilderWithVatTrader(userAnswers = userAnswers).build()

  "CommodityCodeController" - {

    "must return OK and the correct view for a GET" in {

      val application = vatTrader(Some(importJourneyAnswers))

      running(application) {
        val request = FakeRequest(GET, pageRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView(application, form, importJourneyAnswers, NormalMode)(request)
      }
    }

    "must post back to the change URL for a GET in CheckMode" in {

      val application = vatTrader(Some(importJourneyAnswers))

      running(application) {
        val request =
          FakeRequest(GET, vehicledetails.routes.CommodityCodeController.onPageLoad(importNumber, vehicleNumber, CheckMode).url)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView(application, form, importJourneyAnswers, CheckMode)(request)
      }
    }

    "must render the vehicle details inset from the vehicle's details" in {

      val answers = importJourneyAnswers
        .unsafeSet(AddVehicleTypePage(vehicleNumber), AddVehicleType.Hcv)
        .unsafeSet(AddVehicleDetailsHeavyCommercialPage(vehicleNumber), AddVehicleDetailsHeavyCommercial("Volvo", "FH16", "Tractor unit", "Sleeper"))

      val application = vatTrader(Some(answers))

      running(application) {
        val request = FakeRequest(GET, pageRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView(application, form, answers, NormalMode)(request)
        contentAsString(result) must include("Volvo FH16")
      }
    }

    "must link to the commodity code guidance" in {

      val application = vatTrader(Some(importJourneyAnswers))

      running(application) {
        val result = route(application, FakeRequest(GET, pageRoute)).value

        contentAsString(result) must include(application.injector.instanceOf[FrontendAppConfig].findingCommodityCodesUrl)
      }
    }

    "must show a back link on the page" in {

      val application = vatTrader(Some(importJourneyAnswers))

      running(application) {
        val result = route(application, FakeRequest(GET, pageRoute)).value

        contentAsString(result) must include("govuk-back-link")
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = importJourneyAnswers.unsafeSet(CommodityCodePage(vehicleNumber), "8703123456")

      val application = vatTrader(Some(userAnswers))

      running(application) {
        val request = FakeRequest(GET, pageRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView(application, form.fill("8703123456"), userAnswers, NormalMode)(request)
      }
    }

    "must save the commodity code and redirect to the next page" in {

      val (application, mockSessionRepository) = applicationWithMockRepository(importJourneyAnswers)

      running(application) {
        val result = route(application, FakeRequest(POST, pageRoute).withFormUrlEncodedBody("value" -> "8703123456")).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        val saved = savedAnswers(mockSessionRepository)
        saved.get(CommodityCodePage(vehicleNumber)) mustEqual Some("8703123456")
        (saved.data \ "vehicles" \ "1" \ "details" \ "commodityCode").as[String] mustEqual "8703123456"
      }
    }

    "must return a Bad Request and the correct error for invalid data" in {

      List(
        ""            -> "commodityCode.error.required",
        "123"         -> "commodityCode.error.length",
        "87031234567" -> "commodityCode.error.length",
        "8703A23456"  -> "commodityCode.error.format"
      ).foreach { case (value, errorKey) =>
        val application = vatTrader(Some(importJourneyAnswers))

        running(application) {
          val request = FakeRequest(POST, pageRoute).withFormUrlEncodedBody("value" -> value)

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual expectedView(application, form.bind(Map("value" -> value)), importJourneyAnswers, NormalMode)(request)
          contentAsString(result) must include(messages(application)(errorKey))
        }
      }
    }

    "must return OK for a GET for an agent with a VAT agent enrolment" in {

      val application = applicationFor(classOf[FakeAgentIdentifierAction], Some(importJourneyAnswers))

      running(application) {
        status(route(application, FakeRequest(GET, pageRoute)).value) mustEqual OK
      }
    }

    "must return OK for a GET for an agent without enrolments" in {

      val application = applicationFor(classOf[FakeAgentNoEnrolmentsIdentifierAction], Some(importJourneyAnswers))

      running(application) {
        status(route(application, FakeRequest(GET, pageRoute)).value) mustEqual OK
      }
    }

    "must redirect a GET to Unauthorised for a private individual" in {
      assertUnauthorisedOnGet(applicationFor(classOf[FakeIdentifierAction], Some(importJourneyAnswers)))
    }

    "must redirect a POST to Unauthorised for a private individual" in {
      assertUnauthorisedOnPost(applicationFor(classOf[FakeIdentifierAction], Some(importJourneyAnswers)))
    }

    "must redirect a GET to Unauthorised for a non-VAT organisation" in {
      assertUnauthorisedOnGet(applicationFor(classOf[FakeOrganisationIdentifierAction], Some(importJourneyAnswers)))
    }

    "must redirect a POST to Unauthorised for a non-VAT organisation" in {
      assertUnauthorisedOnPost(applicationFor(classOf[FakeOrganisationIdentifierAction], Some(importJourneyAnswers)))
    }

    "must redirect a GET to Unauthorised when the identifier rejects the user" in {
      assertUnauthorisedOnGet(applicationFor(classOf[UnauthorisedIdentifierAction], Some(importJourneyAnswers)))
    }

    "must redirect a GET to Unauthorised if no existing session data is found" in {
      assertUnauthorisedOnGet(vatTrader(None))
    }

    "must redirect a POST to Unauthorised if no existing session data is found" in {
      assertUnauthorisedOnPost(vatTrader(None))
    }

    "must redirect a GET to Unauthorised if draftId is missing" in {
      assertUnauthorisedOnGet(vatTrader(Some(importJourneyAnswers.remove(DraftIdPage).success.value)))
    }

    "must redirect a POST to Unauthorised if draftId is missing" in {
      assertUnauthorisedOnPost(vatTrader(Some(importJourneyAnswers.remove(DraftIdPage).success.value)))
    }

    "must redirect a GET to Unauthorised if IQ1.0 was answered Yes" in {
      assertUnauthorisedOnGet(vatTrader(Some(importJourneyAnswers.unsafeSet(VehicleFromEuPage, true))))
    }

    "must redirect a POST to Unauthorised if IQ1.0 was answered Yes" in {
      assertUnauthorisedOnPost(vatTrader(Some(importJourneyAnswers.unsafeSet(VehicleFromEuPage, true))))
    }

    "must redirect a GET to Unauthorised if IQ1.0 has not been answered" in {
      assertUnauthorisedOnGet(vatTrader(Some(importJourneyAnswers.remove(VehicleFromEuPage).success.value)))
    }

    "must redirect a GET to Unauthorised if the import number in the URL is not one of the user's imports" in {
      assertUnauthorisedOnGet(
        vatTrader(Some(importJourneyAnswers)),
        vehicledetails.routes.CommodityCodeController.onPageLoad(ImportNumber(2), vehicleNumber, NormalMode).url
      )
    }

    "must redirect a GET to Unauthorised if the vehicle number in the URL is not one of the user's vehicles" in {
      assertUnauthorisedOnGet(
        vatTrader(Some(importJourneyAnswers)),
        vehicledetails.routes.CommodityCodeController.onPageLoad(importNumber, VehicleNumber(999), NormalMode).url
      )
    }

    "must redirect a GET to Unauthorised if the vehicle in the URL belongs to a different import" in {
      val answers = importJourneyAnswers
        .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A"), "2" -> Json.obj("importEntryNumber" -> "123456789B")))
        .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 2, "details" -> Json.obj("bringingVehicleNI" -> true))))

      assertUnauthorisedOnGet(vatTrader(Some(answers)))
    }

    "must redirect a GET to Unauthorised if the vehicle has been deleted" in {
      val answers = importJourneyAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1, "deleted" -> true)))

      assertUnauthorisedOnGet(vatTrader(Some(answers)))
    }
  }
}
