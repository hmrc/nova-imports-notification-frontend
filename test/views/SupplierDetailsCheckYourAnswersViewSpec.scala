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
import models.{SupplierNumber, UserAnswers, UserContext}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}
import views.html.SupplierDetailsCheckYourAnswersView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SupplierDetailsCheckYourAnswersViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val answers: UserAnswers = emptyUserAnswers

  val userContext: UserContext = UserContext.from(AffinityGroup.Individual, Enrolments(Set.empty), answers)

  val view: SupplierDetailsCheckYourAnswersView = app.injector.instanceOf[SupplierDetailsCheckYourAnswersView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "SupplierDetailsCheckYourAnswersView" - {

    "must render the correct heading" in {
      val html: String = view(userContext, answers, SupplierNumber(1))(request, msgs).toString

      html must include(msgs("supplierDetailsCheckYourAnswers.heading"))
    }

    "must render the correct page title" in {
      val html: String = view(userContext, answers, SupplierNumber(1))(request, msgs).toString

      html must include(msgs("supplierDetailsCheckYourAnswers.title"))
      html must include("<title>Check the supplier details before adding vehicles - Notification of Vehicle Arrivals - GOV.UK</title>")
    }

    "must render the caption" in {
      val html: String = view(userContext, answers, SupplierNumber(1))(request, msgs).toString

      html must include(msgs("supplierDetailsCheckYourAnswers.caption"))
      html must include("govuk-caption-l")
    }

    "must render the same content via the render method" in {
      val html: String = view.render(userContext, answers, SupplierNumber(1), request, msgs).toString

      html must include(msgs("supplierDetailsCheckYourAnswers.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(userContext, answers, SupplierNumber(1))(request, msgs).toString

      html must include(msgs("supplierDetailsCheckYourAnswers.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }

  }

}
