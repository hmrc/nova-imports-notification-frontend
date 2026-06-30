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

package services

import base.SpecBase
import config.FrontendAppConfig
import connectors.{AddressLookupConnector, AddressLookupError}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsObject
import play.api.test.Helpers.stubMessagesApi
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AddressLookupServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  private val callbackUrl                = "http://localhost:10300/nova-imports/add-address/address-lookup-callback"
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val appConfig: FrontendAppConfig = {
    val m = mock[FrontendAppConfig]
    when(m.signOutUrl).thenReturn("/test/sign-out")
    m
  }

  private def newService(connector: AddressLookupConnector): AddressLookupService =
    new AddressLookupService(connector, stubMessagesApi(), appConfig)

  private def captureConfig(connector: AddressLookupConnector): JsObject = {
    val captor = ArgumentCaptor.forClass(classOf[JsObject])
    verify(connector).initJourney(captor.capture())(any[HeaderCarrier])
    captor.getValue
  }

  "AddressLookupService.initJourney" - {

    "must call the connector and return the journey URL it provides" in {
      val connector = mock[AddressLookupConnector]
      when(connector.initJourney(any[JsObject])(any[HeaderCarrier])).thenReturn(Future.successful(Right("http://alf/journey/abc")))

      val result = newService(connector).initJourney(ukMode = true, callbackUrl).futureValue
      result mustBe Right("http://alf/journey/abc")
    }

    "must propagate the connector's error result" in {
      val connector = mock[AddressLookupConnector]
      val err       = AddressLookupError.UpstreamError(500, "boom")
      when(connector.initJourney(any[JsObject])(any[HeaderCarrier])).thenReturn(Future.successful(Left(err)))

      newService(connector).initJourney(ukMode = false, callbackUrl).futureValue mustBe Left(err)
    }

    "for UK mode" - {

      def buildUkConfig: JsObject = {
        val connector = mock[AddressLookupConnector]
        when(connector.initJourney(any[JsObject])(any[HeaderCarrier])).thenReturn(Future.successful(Right("url")))
        newService(connector).initJourney(ukMode = true, callbackUrl).futureValue
        captureConfig(connector)
      }

      "must set version 2 and ukMode true" in {
        val config = buildUkConfig
        (config \ "version").as[Int] mustBe 2
        (config \ "options" \ "ukMode").as[Boolean] mustBe true
      }

      "must use the callback URL as continueUrl" in {
        (buildUkConfig \ "options" \ "continueUrl").as[String] mustBe callbackUrl
      }

      "must set service-nav + sign-out + heading style options" in {
        val opts = buildUkConfig \ "options"
        (opts \ "signOutHref").as[String] mustBe appConfig.signOutUrl
        (opts \ "useNewGovUkServiceNavigation").as[Boolean] mustBe true
        (opts \ "pageHeadingStyle").as[String] mustBe "govuk-heading-l"
      }

      "must include a 15-minute timeout config pointing at our sign-out and keep-alive routes" in {
        val timeout = buildUkConfig \ "options" \ "timeoutConfig"
        (timeout \ "timeoutAmount").as[Int] mustBe 900
        (timeout \ "timeoutUrl").as[String] mustBe controllers.auth.routes.AuthController.signOut().url
        (timeout \ "timeoutKeepAliveUrl").as[String] mustBe controllers.routes.KeepAliveController.keepAlive().url
      }

      "must NOT include allowedCountryCodes" in {
        (buildUkConfig \ "options" \ "allowedCountryCodes").toOption mustBe None
      }

      "must mandate addressLine1 and line2 at the ALF UI level (line2 enforced at the FE callback guard so postcode lookup with 3-section addresses isn't blocked)" in {
        val mf = buildUkConfig \ "options" \ "manualAddressEntryConfig" \ "mandatoryFields"
        (mf \ "addressLine1").as[Boolean] mustBe true
        (mf \ "addressLine2").as[Boolean] mustBe true
        (mf \ "town").toOption mustBe None
        (mf \ "postcode").toOption mustBe None
      }

      "must include lookup, select, edit and confirm labels" in {
        val labelsEn = buildUkConfig \ "labels" \ "en"
        (labelsEn \ "lookupPageLabels").toOption mustBe defined
        (labelsEn \ "selectPageLabels").toOption mustBe defined
        (labelsEn \ "editPageLabels").toOption mustBe defined
        (labelsEn \ "confirmPageLabels").toOption mustBe defined
      }

      "must hide the search-again link on the select page (prototype shows None of these only)" in {
        val select = buildUkConfig \ "options" \ "selectPageConfig"
        (select \ "showSearchAgainLink").as[Boolean] mustBe false
        (select \ "showSearchLinkAgain").toOption mustBe None
      }

      "must hide search-again and confirm-change text on the confirm page" in {
        val confirm = buildUkConfig \ "options" \ "confirmPageConfig"
        (confirm \ "showChangeLink").as[Boolean] mustBe true
        (confirm \ "showSearchAgainLink").toOption mustBe None
        (confirm \ "showConfirmChangeText").toOption mustBe None
      }

      "must include maxLengthErrorMessages for line1/line2/line3/town in both en and cy" in {
        val maxLen = buildUkConfig \ "options" \ "manualAddressEntryConfig" \ "maxLengthErrorMessages"
        for (lang <- Seq("en", "cy"); field <- Seq("addressLine1", "addressLine2", "addressLine3", "town"))
          (maxLen \ lang \ field).asOpt[String] mustBe defined
      }

      "must override editPage.town.error via otherLabels and not override country" in {
        val otherEn = buildUkConfig \ "labels" \ "en" \ "otherLabels"
        (otherEn \ "editPage.town.error").asOpt[String] mustBe defined
        (otherEn \ "constants.editPageCountryErrorMessage").toOption mustBe None
      }
    }

    "for non-UK mode" - {

      def buildNonUkConfig: JsObject = {
        val connector = mock[AddressLookupConnector]
        when(connector.initJourney(any[JsObject])(any[HeaderCarrier])).thenReturn(Future.successful(Right("url")))
        newService(connector).initJourney(ukMode = false, callbackUrl).futureValue
        captureConfig(connector)
      }

      "must set ukMode false" in {
        (buildNonUkConfig \ "options" \ "ukMode").as[Boolean] mustBe false
      }

      "must include the notifier-allowed country codes (Nitish's list — all countries including GB and HR)" in {
        val codes = (buildNonUkConfig \ "options" \ "allowedCountryCodes").as[Seq[String]]
        codes      must contain allOf ("GB", "HR", "GR", "DE", "FR", "US", "IE")
        codes      must not contain "EL"
        codes.size must be > 200
      }

      "must mandate addressLine1 and line2 at the ALF UI level (line2 enforced at the FE callback guard for consistency with the UK flow)" in {
        val mf = buildNonUkConfig \ "options" \ "manualAddressEntryConfig" \ "mandatoryFields"
        (mf \ "addressLine1").as[Boolean] mustBe true
        (mf \ "addressLine2").as[Boolean] mustBe true
        (mf \ "town").toOption mustBe None
        (mf \ "postcode").toOption mustBe None
        (mf \ "country").toOption mustBe None
      }

      "must hide confirm-change text on the confirm page" in {
        val confirm = buildNonUkConfig \ "options" \ "confirmPageConfig"
        (confirm \ "showChangeLink").as[Boolean] mustBe true
        (confirm \ "showConfirmChangeText").toOption mustBe None
      }

      "must put edit and confirm labels under the international namespace (ALF's international template uses international.* keys)" in {
        val labelsEn = buildNonUkConfig \ "labels" \ "en"
        (labelsEn \ "editPageLabels").toOption mustBe None
        (labelsEn \ "confirmPageLabels").toOption mustBe None
        (labelsEn \ "lookupPageLabels").toOption mustBe None
        (labelsEn \ "selectPageLabels").toOption mustBe None
        (labelsEn \ "international" \ "editPageLabels").toOption mustBe defined
        (labelsEn \ "international" \ "editPageLabels" \ "postcodeLabel").asOpt[String] mustBe defined
        (labelsEn \ "international" \ "confirmPageLabels").toOption mustBe defined
      }

      "must include maxLengthErrorMessages for line1/line2/line3/town in both en and cy" in {
        val maxLen = buildNonUkConfig \ "options" \ "manualAddressEntryConfig" \ "maxLengthErrorMessages"
        for (lang <- Seq("en", "cy"); field <- Seq("addressLine1", "addressLine2", "addressLine3", "town"))
          (maxLen \ lang \ field).asOpt[String] mustBe defined
      }

      "must override editPage.town.error and editPageCountryErrorMessage via otherLabels" in {
        val otherEn = buildNonUkConfig \ "labels" \ "en" \ "otherLabels"
        (otherEn \ "editPage.town.error").asOpt[String] mustBe defined
        (otherEn \ "constants.editPageCountryErrorMessage").asOpt[String] mustBe defined
      }
    }
  }
}
