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
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.VehicleOutsideEUView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VehicleOutsideEUViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: VehicleOutsideEUView = app.injector.instanceOf[VehicleOutsideEUView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "VehicleOutsideEUView" - {

    "must render the correct heading" in {
      val html: String = view.apply("https://example.com/importing").toString

      html must include(msgs("vehicleOutsideEU.heading"))
    }

    "must render the correct page title" in {
      val html: String = view.apply("https://example.com/importing").toString

      html must include(msgs("vehicleOutsideEU.title"))
    }

    "must render the first paragraph" in {
      val html: String = view.apply("https://example.com/importing").toString

      html must include(msgs("vehicleOutsideEU.paragraph.1"))
    }

    "must include the importing vehicles URL in the second paragraph" in {
      val importingUrl: String = "https://example.com/importing-vehicles"
      val html: String         = view.apply(importingUrl).toString

      html must include(importingUrl)
    }

    "must render the same content via the render method" in {
      val html: String = view.render("https://example.com/importing", request, msgs).toString

      html must include(msgs("vehicleOutsideEU.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f("https://example.com/importing")(request, msgs).toString

      html must include(msgs("vehicleOutsideEU.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }

}
