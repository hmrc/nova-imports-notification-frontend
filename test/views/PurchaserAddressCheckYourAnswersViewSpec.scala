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
import controllers.purchaseraddress.routes
import models.{Address, Country, UserAnswers}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import pages.sections.purchaseraddress.PurchaserAddressPage
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.PurchaserAddressCheckYourAnswersView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PurchaserAddressCheckYourAnswersViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val address: Address = Address(
    lines = Seq("12 High Street", "Reading"),
    postcode = Some("RE12 9GC"),
    country = Country("GB", "United Kingdom")
  )

  val answers: UserAnswers = emptyUserAnswers.unsafeSet(PurchaserAddressPage, address)

  val view: PurchaserAddressCheckYourAnswersView = app.injector.instanceOf[PurchaserAddressCheckYourAnswersView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "PurchaserAddressCheckYourAnswersView" - {

    "must render the correct heading" in {
      val html: String = view(answers)(request, msgs).toString

      html must include(msgs("purchaserAddressCheckYourAnswers.heading"))
    }

    "must render the correct page title" in {
      val html: String = view(answers)(request, msgs).toString

      html must include(msgs("purchaserAddressCheckYourAnswers.title"))
    }

    "must render the caption" in {
      val html: String = view(answers)(request, msgs).toString

      html must include(msgs("purchaserAddressCheckYourAnswers.caption"))
      html must include("govuk-caption-l")
    }

    "must render the address with each line on its own line" in {
      val html: String = view(answers)(request, msgs).toString

      html must include("12 High Street<br>Reading<br>RE12 9GC")
    }

    "must send the change link back to the start of the address journey" in {
      val html: String = view(answers)(request, msgs).toString

      html must include(routes.PurchaserAddressCheckYourAnswersController.onChangeAddress().url)
    }

    "must render a continue button" in {
      val html: String = view(answers)(request, msgs).toString

      html must include(msgs("site.continue"))
    }

    "must render the same content via the render method" in {
      val html: String = view.render(answers, request, msgs).toString

      html must include(msgs("purchaserAddressCheckYourAnswers.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(answers)(request, msgs).toString

      html must include(msgs("purchaserAddressCheckYourAnswers.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }

}
