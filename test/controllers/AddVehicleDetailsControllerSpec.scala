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
import com.google.inject.name.Names
import config.FrontendAppConfig
import controllers.actions.*
import forms.AddVehicleDetailsFormProvider
import models.requests.IdentifierRequest
import models.{AddVehicleDetails, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{AddVehicleDetailsPage, VehicleFromEuPage}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, EnrolmentIdentifier, Enrolments}
import views.html.AddVehicleDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddVehicleDetailsControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new AddVehicleDetailsFormProvider()
  val form         = formProvider()

  lazy val addVehicleDetailsRoute = routes.AddVehicleDetailsController.onPageLoad(NormalMode).url

  val userAnswersWithIq1Yes: UserAnswers = emptyUserAnswers.set(VehicleFromEuPage, true).success.value

  private def spreadsheetUrl(app: play.api.Application): String =
    app.injector.instanceOf[FrontendAppConfig].multipleVehiclesSpreadsheetsUrl

  private def applicationFor(
    standardIdentifier: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers]
  ): play.api.Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(standardIdentifier),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )
      .build()

  "AddVehicleDetailsController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithIq1Yes)).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddVehicleDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, spreadsheetUrl(application))(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithIq1Yes
        .set(AddVehicleDetailsPage, AddVehicleDetails.BySupplier)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val view = application.injector.instanceOf[AddVehicleDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(AddVehicleDetails.BySupplier), NormalMode, spreadsheetUrl(application))(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithIq1Yes))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, addVehicleDetailsRoute)
            .withFormUrlEncodedBody(("value", AddVehicleDetails.BySupplier.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithIq1Yes)).build()

      running(application) {
        val request =
          FakeRequest(POST, addVehicleDetailsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[AddVehicleDetailsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, spreadsheetUrl(application))(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, addVehicleDetailsRoute)
            .withFormUrlEncodedBody(("value", AddVehicleDetails.BySupplier.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if IQ1 has not been answered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if IQ1 was answered No" in {

      val answersIq1No = emptyUserAnswers.set(VehicleFromEuPage, false).success.value
      val application  = applicationBuilder(userAnswers = Some(answersIq1No)).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when the user is an OGD agent (user type 7 or 8)" in {

      val application = applicationFor(classOf[UnauthorisedIdentifierAction], Some(userAnswersWithIq1Yes))

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST when the user is an OGD agent (user type 7 or 8)" in {

      val application = applicationFor(classOf[UnauthorisedIdentifierAction], Some(userAnswersWithIq1Yes))

      running(application) {
        val request =
          FakeRequest(POST, addVehicleDetailsRoute)
            .withFormUrlEncodedBody(("value", AddVehicleDetails.BySupplier.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when the user is a de-registered organisation (user type 9)" in {

      val application = applicationFor(classOf[FakeDeregisteredOrganisationIdentifierAction], Some(userAnswersWithIq1Yes))

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST when the user is a de-registered organisation (user type 9)" in {

      val application = applicationFor(classOf[FakeDeregisteredOrganisationIdentifierAction], Some(userAnswersWithIq1Yes))

      running(application) {
        val request =
          FakeRequest(POST, addVehicleDetailsRoute)
            .withFormUrlEncodedBody(("value", AddVehicleDetails.BySupplier.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}

class FakeDeregisteredOrganisationIdentifierAction @Inject() (bodyParsers: PlayBodyParsers) extends IdentifierAction {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] =
    block(
      IdentifierRequest(
        request,
        "id",
        AffinityGroup.Organisation,
        Enrolments(Set(Enrolment("HMCE-VATDEC-ORG", Seq(EnrolmentIdentifier("VATRegNo", "700037204")), "NotYetActivated")))
      )
    )

  override def parser: BodyParser[AnyContent] =
    bodyParsers.default

  override protected def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}
