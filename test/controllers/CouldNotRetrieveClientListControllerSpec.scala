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
import controllers.actions.*
import models.requests.IdentifierRequest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CouldNotRetrieveClientListControllerSpec extends SpecBase {

  "CouldNotRetrieveClientListController" - {

    "onPageLoad" - {

      "must return OK and render the correct view" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.CouldNotRetrieveClientListController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("We could not retrieve your client list")
        }
      }

      "must render the bullet point reasons" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.CouldNotRetrieveClientListController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("there is a temporary problem with the service")
          contentAsString(result) must include("your authorisations are still being processed")
          contentAsString(result) must include("you do not have any authorised clients linked to your agent code")
        }
      }

      "must render the HMRC Online Services Helpdesk link" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.CouldNotRetrieveClientListController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("HMRC Online Services Helpdesk (opens in new tab).")
          contentAsString(result) must include("https://www.gov.uk/government/organisations/hm-revenue-customs/contact/online-services-helpdesk")
        }
      }

      "must render the return to home button" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.CouldNotRetrieveClientListController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Return to home")
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.CouldNotRetrieveClientListController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to unauthorised if the user is not a VAT agent" in {
        given application: Application = new GuiceApplicationBuilder()
          .overrides(
            bind[DataRequiredAction].to[DataRequiredActionImpl],
            bind[IdentifierAction].to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("vatAgent")).to[FakeUnauthorisedVatAgentAction],
            bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
            bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(emptyUserAnswers)))
          )
          .build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.CouldNotRetrieveClientListController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }
  }
}

class FakeUnauthorisedVatAgentAction @Inject() (bodyParsers: PlayBodyParsers) extends IdentifierAction {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] =
    Future.successful(Results.Redirect(routes.UnauthorisedController.onPageLoad()))

  override def parser: BodyParser[AnyContent] =
    bodyParsers.default

  override protected def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}
