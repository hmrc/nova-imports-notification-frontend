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
import controllers.routes
import models.{DraftNotification, NormalMode, SectionStatus}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.NotificationTaskListView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class NotificationTaskListViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: NotificationTaskListView = app.injector.instanceOf[NotificationTaskListView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val traderName = Some("Harbourview Limited")
  val vrn        = Some("123456789")

  private val allNotYetSaved: Map[String, SectionStatus] = Map(
    DraftNotification.SectionId.NotifierDetails -> SectionStatus.NotYetSaved,
    DraftNotification.SectionId.NotifierAddress -> SectionStatus.NotYetSaved,
    DraftNotification.SectionId.Vehicles        -> SectionStatus.NotYetSaved,
    DraftNotification.SectionId.Declaration     -> SectionStatus.NotYetSaved
  )

  private val sectionLinks: Map[String, String] = Map(
    DraftNotification.SectionId.NotifierDetails -> routes.AboutYourDetailsController.onPageLoad().url,
    DraftNotification.SectionId.NotifierAddress -> routes.IsYourAddressInTheUkController.onPageLoad(NormalMode).url,
    DraftNotification.SectionId.Vehicles        -> routes.AddVehicleDetailsController.onPageLoad(NormalMode).url,
    DraftNotification.SectionId.Declaration     -> "/some-url"
  )

  "NotificationTaskListView" - {

    "must render the page heading, trader name and VRN caption" in {
      val html: String = view(traderName, vrn, allNotYetSaved, showAddYourAddress = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.heading"))
      html must include(traderName.get)
      html must include(msgs("notificationTaskList.vrn.caption", vrn.get))
    }

    "must omit the trader caption entirely when name and VRN are both absent" in {
      val html: String = view(None, None, allNotYetSaved, showAddYourAddress = false, sectionLinks).toString
      html must not include "govuk-caption-l"
      html must not include msgs("notificationTaskList.vrn.caption", vrn)
    }

    "must render the page title" in {
      val html: String = view(traderName, vrn, allNotYetSaved, showAddYourAddress = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.title"))
    }

    "must render the About you section with an Add your details link to AYD1.0" in {
      val html: String = view(traderName, vrn, allNotYetSaved, showAddYourAddress = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.aboutYou.heading"))
      html must include(msgs("notificationTaskList.aboutYou.addYourDetails"))
      html must include(routes.AboutYourDetailsController.onPageLoad().url)
    }

    "must omit the Add your address row when showAddYourAddress is false" in {
      val html: String = view(traderName, vrn, allNotYetSaved, showAddYourAddress = false, sectionLinks).toString
      html must not include msgs("notificationTaskList.aboutYou.addYourAddress")
    }

    "must render the Add your address row linking to AYA1.0 when showAddYourAddress is true" in {
      val html: String = view(traderName, vrn, allNotYetSaved, showAddYourAddress = true, sectionLinks).toString
      html must include(msgs("notificationTaskList.aboutYou.addYourAddress"))
      html must include(routes.IsYourAddressInTheUkController.onPageLoad(NormalMode).url)
    }

    "must render the About the vehicles section with the Add vehicle details task linking to AVD1.0" in {
      val html: String = view(traderName, vrn, allNotYetSaved, showAddYourAddress = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.aboutTheVehicles.heading"))
      html must include(msgs("notificationTaskList.aboutTheVehicles.addVehicleDetails"))
      html must include(routes.AddVehicleDetailsController.onPageLoad(NormalMode).url)
    }

    "must render the Declaration section with Cannot start yet status and the hint" in {
      val html: String = view(traderName, vrn, allNotYetSaved, showAddYourAddress = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.declaration.heading"))
      html must include(msgs("notificationTaskList.declaration.readDeclaration"))
      html must include(msgs("notificationTaskList.declaration.hint"))
      html must include(msgs("notificationTaskList.status.cannotStartYet"))
    }

    "must mark sections with status not-yet-saved as Incomplete" in {
      val html: String = view(traderName, vrn, allNotYetSaved, showAddYourAddress = true, sectionLinks).toString
      html must include(msgs("notificationTaskList.status.incomplete"))
    }

    "must mark sections with status completed as Completed" in {
      val sections     = allNotYetSaved + (DraftNotification.SectionId.NotifierDetails -> SectionStatus.Completed)
      val html: String = view(traderName, vrn, sections, showAddYourAddress = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.status.completed"))
    }

    "must default to Incomplete when a section is missing from the response" in {
      val html: String = view(traderName, vrn, Map.empty, showAddYourAddress = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.status.incomplete"))
    }

    "must render the Return to home link to the landing page" in {
      val html: String = view(traderName, vrn, allNotYetSaved, showAddYourAddress = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.returnToHome"))
      html must include(routes.LandingPageController.onPageLoad().url)
    }

    "must render the Delete notification warning button" in {
      val html: String = view(traderName, vrn, allNotYetSaved, showAddYourAddress = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.deleteNotification"))
      html must include("govuk-button--warning")
    }

    "must render the same content via the render method" in {
      val html: String = view.render(traderName, vrn, allNotYetSaved, false, sectionLinks, request, msgs).toString
      html must include(msgs("notificationTaskList.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(traderName, vrn, allNotYetSaved, false, sectionLinks)(request, msgs).toString
      html must include(msgs("notificationTaskList.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
