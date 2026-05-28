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

package views

import base.SpecBase
import models.{UserAnswers, UserContext}
import org.scalatest.matchers.must.Matchers
import pages.VehicleFromEuPage
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}
import views.html.InitialQuestionsCheckYourAnswersView

class InitialQuestionsCheckYourAnswersViewSpec extends SpecBase with Matchers {

  "InitialQuestionsCheckYourAnswersView" - {

    "must render the correct heading" in new Setup {
      val html: String = view(userContext, answers)(request, msgs).toString

      html must include(msgs("initialQuestionsCheckYourAnswers.heading"))
    }

    "must render the correct page title" in new Setup {
      val html: String = view(userContext, answers)(request, msgs).toString

      html must include(msgs("initialQuestionsCheckYourAnswers.title"))
    }

    "must render the caption" in new Setup {
      val html: String = view(userContext, answers)(request, msgs).toString

      html must include(msgs("initialQuestionsCheckYourAnswers.caption"))
      html must include("govuk-caption-l")
    }

    "must render the same content via the render method" in new Setup {
      val html: String = view.render(userContext, answers, request, msgs).toString

      html must include(msgs("initialQuestionsCheckYourAnswers.heading"))
    }

    "must render the same content via the f method" in new Setup {
      val html: String = view.f(userContext, answers)(request, msgs).toString

      html must include(msgs("initialQuestionsCheckYourAnswers.heading"))
    }

    "must return itself via the ref method" in new Setup {
      view.ref mustBe view
    }
  }

  trait Setup {
    val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
    implicit val request: Request[?] = FakeRequest()
    implicit val msgs: Messages      = messages(app)

    val answers: UserAnswers     = emptyUserAnswers.set(VehicleFromEuPage, true).success.value
    val userContext: UserContext = UserContext.from(AffinityGroup.Individual, Enrolments(Set.empty), answers)

    val view: InitialQuestionsCheckYourAnswersView = app.injector.instanceOf[InitialQuestionsCheckYourAnswersView]
  }
}
