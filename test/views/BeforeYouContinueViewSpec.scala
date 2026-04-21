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
import models.NormalMode
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.BeforeYouContinueView

class BeforeYouContinueViewSpec extends SpecBase with Matchers {

  "BeforeYouContinueView" - {

    "must render the correct heading" in new Setup {
      val html: String = view().toString
      html must include(msgs("beforeYouContinue.heading"))
    }

    "must render the correct page title" in new Setup {
      val html: String = view().toString
      html must include(msgs("beforeYouContinue.title"))
    }

    "must render the intro body" in new Setup {
      val html: String = view().toString
      html must include(msgs("beforeYouContinue.body.1"))
    }

    "must render the vehicle section body and all six bulleted items" in new Setup {
      val html: String = view().toString
      html must include(msgs("beforeYouContinue.vehicle.body"))
      html must include(msgs("beforeYouContinue.vehicle.list.1"))
      html must include(msgs("beforeYouContinue.vehicle.list.2"))
      html must include(msgs("beforeYouContinue.vehicle.list.3"))
      html must include(msgs("beforeYouContinue.vehicle.list.4"))
      html must include(msgs("beforeYouContinue.vehicle.list.5"))
      html must include(msgs("beforeYouContinue.vehicle.list.6"))
    }

    "must render the supplier section body and both bulleted items" in new Setup {
      val html: String = view().toString
      html must include(msgs("beforeYouContinue.supplier.body"))
      html must include(msgs("beforeYouContinue.supplier.list.1"))
      html must include(msgs("beforeYouContinue.supplier.list.2"))
    }

    "must render the Updating vehicle notifications H2 and both paragraphs" in new Setup {
      val html: String = view().toString
      html must include(msgs("beforeYouContinue.updating.heading"))
      html must include(msgs("beforeYouContinue.updating.body.1"))
      html must include(msgs("beforeYouContinue.updating.body.2"))
    }

    "must render the Continue button as a link to the VehicleFromEu page" in new Setup {
      val html: String = view().toString
      html must include(msgs("site.continue"))
      html must include(controllers.routes.VehicleFromEuController.onPageLoad(NormalMode).url)
    }

    "must render the same content via the render method" in new Setup {
      val html: String = view.render(request, msgs).toString
      html must include(msgs("beforeYouContinue.heading"))
    }

    "must render the same content via the f method" in new Setup {
      val html: String = view.f()(request, msgs).toString
      html must include(msgs("beforeYouContinue.heading"))
    }

    "must return itself via the ref method" in new Setup {
      view.ref mustBe view
    }
  }

  trait Setup {
    val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
    implicit val request: Request[?] = FakeRequest()
    implicit val msgs: Messages      = messages(app)

    val view: BeforeYouContinueView = app.injector.instanceOf[BeforeYouContinueView]
  }
}
