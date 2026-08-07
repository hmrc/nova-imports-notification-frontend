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
import models.{Address, Country}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.PurchaserAddressChangedView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PurchaserAddressChangedViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: PurchaserAddressChangedView = app.injector.instanceOf[PurchaserAddressChangedView]

  private val ukAddress = Address(
    lines = Seq("23 North Road", "East London", "London"),
    postcode = Some("ER45 6UI"),
    country = Country("GB", "United Kingdom")
  )

  private val nonUkAddress = Address(
    lines = Seq("Musterstrasse 12", "Berlin"),
    postcode = None,
    country = Country("DE", "Germany")
  )

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "PurchaserAddressChangedView" - {

    "must render the correct heading" in {
      val html: String = view(ukAddress)(request, msgs).toString

      html must include(msgs("purchaserAddressChanged.heading"))
    }

    "must render the correct page title" in {
      val html: String = view(ukAddress)(request, msgs).toString

      html must include(msgs("purchaserAddressChanged.title"))
    }

    "must render the 'Add purchaser address' caption" in {
      val html: String = view(ukAddress)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("purchaserAddressChanged.caption"))
    }

    "must render the explanatory body and check-address subheading" in {
      val html: String = view(ukAddress)(request, msgs).toString

      html must include(msgs("purchaserAddressChanged.body"))
      html must include(msgs("purchaserAddressChanged.checkHeading"))
    }

    "must render each address line, the postcode and the country for a UK address" in {
      val html: String = view(ukAddress)(request, msgs).toString

      html must include("23 North Road")
      html must include("East London")
      html must include("London")
      html must include("ER45 6UI")
      html must include("United Kingdom")
    }

    "must not render a postcode for a non-UK address" in {
      val html: String = view(nonUkAddress)(request, msgs).toString

      html must include("Musterstrasse 12")
      html must include("Berlin")
      html must include("Germany")
      html must not include "ER45 6UI"
    }

    "must render the change address link pointing at APA1.0" in {
      val html: String = view(ukAddress)(request, msgs).toString

      html must include(msgs("purchaserAddressChanged.changeAddress"))
      html must include(controllers.routes.PurchaserAddressChangedController.onChangeAddress().url)
    }

    "must render the Save and continue button" in {
      val html: String = view(ukAddress)(request, msgs).toString

      html must include(msgs("site.saveAndContinue"))
    }

    "must post to the PurchaserAddressChanged submit URL" in {
      val html: String = view(ukAddress)(request, msgs).toString

      html must include(s"""action="${controllers.routes.PurchaserAddressChangedController.onSubmit().url}"""")
    }

    "must render the same content via the render method" in {
      val html: String = view.render(ukAddress, request, msgs).toString

      html must include(msgs("purchaserAddressChanged.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(ukAddress)(request, msgs).toString

      html must include(msgs("purchaserAddressChanged.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }

}
