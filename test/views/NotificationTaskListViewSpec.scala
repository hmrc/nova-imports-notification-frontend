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
import controllers.{notifieraddress, notifierdetails, purchaseraddress, purchaserdetails, routes, vehicledetails}
import models.{AgentSelectedClient, DraftNotification, NormalMode, SectionStatus}
import org.jsoup.Jsoup
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
    DraftNotification.SectionId.NotifierDetails  -> notifierdetails.routes.AboutYourDetailsController.onPageLoad().url,
    DraftNotification.SectionId.NotifierAddress  -> notifieraddress.routes.IsYourAddressInTheUkController.onPageLoad(NormalMode).url,
    DraftNotification.SectionId.PurchaserDetails -> purchaserdetails.routes.PurchaserNameController.onPageLoad(NormalMode).url,
    DraftNotification.SectionId.PurchaserAddress -> purchaseraddress.routes.IsPurchaserAddressInTheUkController.onPageLoad(NormalMode).url,
    DraftNotification.SectionId.Vehicles         -> vehicledetails.routes.AddVehicleDetailsController.onPageLoad(NormalMode).url,
    DraftNotification.SectionId.Declaration      -> "/some-url"
  )

  "NotificationTaskListView" - {

    "must render the page heading, trader name and VRN caption" in {
      val html: String = view(traderName, vrn, None, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.heading"))
      html must include(traderName.get)
      html must include(msgs("notificationTaskList.vrn.caption", vrn.get))
    }

    "must omit the trader caption entirely when name and VRN are both absent" in {
      val html: String = view(None, None, None, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      html must not include "govuk-caption-l"
      html must not include msgs("notificationTaskList.vrn.caption", vrn)
    }

    "must render the page title" in {
      val html: String = view(traderName, vrn, None, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.title"))
    }

    "must render the About you section with an Add your details link to AYD1.0" in {
      val html: String = view(traderName, vrn, None, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.aboutYou.heading"))
      html must include(msgs("notificationTaskList.aboutYou.addYourDetails"))
      html must include(notifierdetails.routes.AboutYourDetailsController.onPageLoad().url)
    }

    "must omit the Add your address row when showAddYourAddress is false" in {
      val html: String = view(traderName, vrn, None, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      html must not include msgs("notificationTaskList.aboutYou.addYourAddress")
    }

    "must render the Add your address row linking to AYA1.0 when showAddYourAddress is true" in {
      val html: String = view(traderName, vrn, None, allNotYetSaved, showAddYourAddress = true, showAboutThePurchaser = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.aboutYou.addYourAddress"))
      html must include(notifieraddress.routes.IsYourAddressInTheUkController.onPageLoad(NormalMode).url)
    }

    "must render the About the vehicles section with the Add vehicle details task linking to AVD1.0" in {
      val html: String = view(traderName, vrn, None, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.aboutTheVehicles.heading"))
      html must include(msgs("notificationTaskList.aboutTheVehicles.addVehicleDetails"))
      html must include(vehicledetails.routes.AddVehicleDetailsController.onPageLoad(NormalMode).url)
    }

    "must render the Declaration section with Cannot start yet status and the hint" in {
      val html: String = view(traderName, vrn, None, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.declaration.heading"))
      html must include(msgs("notificationTaskList.declaration.readDeclaration"))
      html must include(msgs("notificationTaskList.declaration.hint"))
      html must include(msgs("notificationTaskList.status.cannotStartYet"))
    }

    "must render the Declaration hint as a paragraph" in {
      val doc =
        org.jsoup.Jsoup.parse(
          view(traderName, vrn, None, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
        )
      val hint = doc.getElementById("declaration-1-hint")

      hint.tagName mustEqual "p"
      hint.text mustEqual msgs("notificationTaskList.declaration.hint")
      doc.getElementById("declaration-1-status").text mustEqual msgs("notificationTaskList.status.cannotStartYet")
      doc.select("a:contains(" + msgs("notificationTaskList.declaration.readDeclaration") + ")").size mustEqual 0
    }

    "must mark sections with status not-yet-saved as Incomplete" in {
      val html: String = view(traderName, vrn, None, allNotYetSaved, showAddYourAddress = true, showAboutThePurchaser = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.status.incomplete"))
    }

    "must mark sections with status completed as Completed" in {
      val sections     = allNotYetSaved + (DraftNotification.SectionId.NotifierDetails -> SectionStatus.Completed)
      val html: String = view(traderName, vrn, None, sections, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.status.completed"))
    }

    "must default to Incomplete when a section is missing from the response" in {
      val html: String = view(traderName, vrn, None, Map.empty, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.status.incomplete"))
    }

    "must render the Return to home link to the landing page" in {
      val html: String = view(traderName, vrn, None, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.returnToHome"))
      html must include(routes.LandingPageController.onPageLoad().url)
    }

    "must render the Delete notification warning button" in {
      val html: String = view(traderName, vrn, None, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      html must include(msgs("notificationTaskList.deleteNotification"))
      html must include("govuk-button--warning")
    }

    "must omit the About the purchaser section when showAboutThePurchaser is false" in {
      val html: String = view(traderName, vrn, None, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      html must not include msgs("notificationTaskList.aboutThePurchaser.heading")
      html must not include msgs("notificationTaskList.aboutThePurchaser.addPurchaserDetails")
    }

    "must render the About the purchaser section with both tasks and their links when showAboutThePurchaser is true" in {
      val html: String = view(traderName, vrn, None, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = true, sectionLinks).toString
      html must include(msgs("notificationTaskList.aboutThePurchaser.heading"))
      html must include(msgs("notificationTaskList.aboutThePurchaser.addPurchaserDetails"))
      html must include(msgs("notificationTaskList.aboutThePurchaser.addPurchaserAddress"))
      html must include(purchaserdetails.routes.PurchaserNameController.onPageLoad(NormalMode).url)
      html must include(purchaseraddress.routes.IsPurchaserAddressInTheUkController.onPageLoad(NormalMode).url)
    }

    "must omit the client summary card when no client is supplied" in {
      val html: String = view(traderName, vrn, None, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      html must not include "govuk-summary-card"
    }

    "must render the client summary card with the client name in the title and the GB-prefixed VRN" in {
      val client = Some(AgentSelectedClient(vrn = "123456789", name = Some("BMD Logistics")))
      val doc    = Jsoup.parse(
        view(traderName, None, client, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      )

      doc.select("h2.govuk-summary-card__title").text mustEqual "Notifying on behalf of BMD Logistics"
      doc.select(".govuk-summary-card .govuk-summary-list__key").text mustEqual "VAT registration number"
      doc.select(".govuk-summary-card .govuk-summary-list__value").text mustEqual "GB123456789"
    }

    "must render the client summary card title without a name when the client name is unknown" in {
      val client = Some(AgentSelectedClient(vrn = "123456789", name = None))
      val doc    = Jsoup.parse(
        view(traderName, None, client, allNotYetSaved, showAddYourAddress = false, showAboutThePurchaser = false, sectionLinks).toString
      )

      doc.select("h2.govuk-summary-card__title").text mustEqual "Notifying on behalf of"
      doc.select(".govuk-summary-card .govuk-summary-list__value").text mustEqual "GB123456789"
    }

    "must render the same content via the render method" in {
      val html: String = view.render(traderName, vrn, None, allNotYetSaved, false, false, sectionLinks, request, msgs).toString
      html must include(msgs("notificationTaskList.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(traderName, vrn, None, allNotYetSaved, false, false, sectionLinks)(request, msgs).toString
      html must include(msgs("notificationTaskList.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
