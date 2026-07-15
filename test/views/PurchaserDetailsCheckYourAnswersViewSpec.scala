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
import models.{NameDetails, UserAnswers, UserContext}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import pages.sections.purchaserDetails.{PurchaserBusinessNamePage, PurchaserNamePage}
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}
import views.html.PurchaserDetailsCheckYourAnswersView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PurchaserDetailsCheckYourAnswersViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val individualAnswers: UserAnswers = emptyUserAnswers
    .set(PurchaserNamePage, NameDetails("Mr", "John", "Smith"))
    .success
    .value

  val businessAnswers: UserAnswers = emptyUserAnswers
    .set(PurchaserBusinessNamePage, "Acme Trading Ltd")
    .success
    .value

  val userContext: UserContext = UserContext.from(AffinityGroup.Individual, Enrolments(Set.empty), individualAnswers)

  val view: PurchaserDetailsCheckYourAnswersView = app.injector.instanceOf[PurchaserDetailsCheckYourAnswersView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "PurchaserDetailsCheckYourAnswersView" - {

    "must render the correct heading" in {
      val html: String = view(userContext, individualAnswers)(request, msgs).toString

      html must include(msgs("purchaserDetailsCheckYourAnswers.heading"))
    }

    "must render the correct page title" in {
      val html: String = view(userContext, individualAnswers)(request, msgs).toString

      html must include(msgs("purchaserDetailsCheckYourAnswers.title"))
    }

    "must render the caption" in {
      val html: String = view(userContext, individualAnswers)(request, msgs).toString

      html must include(msgs("purchaserDetailsCheckYourAnswers.caption"))
      html must include("govuk-caption-l")
    }

    "must render the purchaser name row for an individual purchaser" in {
      val html: String = view(userContext, individualAnswers)(request, msgs).toString

      html must include(msgs("purchaserName.checkYourAnswersLabel"))
      html must (include("Mr") and include("John") and include("Smith"))
    }

    "must render the purchaser business name row for a business purchaser" in {
      val html: String = view(userContext, businessAnswers)(request, msgs).toString

      html must include(msgs("purchaserBusinessName.checkYourAnswersLabel"))
      html must include("Acme Trading Ltd")
    }

    "must render the save and continue button" in {
      val html: String = view(userContext, individualAnswers)(request, msgs).toString

      html must include(msgs("site.saveAndContinue"))
    }

    "must render the same content via the render method" in {
      val html: String = view.render(userContext, individualAnswers, request, msgs).toString

      html must include(msgs("purchaserDetailsCheckYourAnswers.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(userContext, individualAnswers)(request, msgs).toString

      html must include(msgs("purchaserDetailsCheckYourAnswers.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }

}
