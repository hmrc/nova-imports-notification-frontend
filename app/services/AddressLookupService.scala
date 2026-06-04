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

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.{AddressLookupConnector, AddressLookupError}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AddressLookupService @Inject() (
  connector: AddressLookupConnector,
  messagesApi: MessagesApi,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext) {

  private val notifierAllowedCountryCodes: Seq[String] = Seq(
    "AF",
    "AX",
    "AL",
    "DZ",
    "AS",
    "AD",
    "AO",
    "AI",
    "AQ",
    "AG",
    "AR",
    "AM",
    "AW",
    "AU",
    "AT",
    "AZ",
    "BS",
    "BH",
    "BD",
    "BB",
    "BY",
    "BE",
    "BZ",
    "BJ",
    "BM",
    "BT",
    "BO",
    "BQ",
    "BA",
    "BW",
    "BV",
    "BR",
    "IO",
    "BN",
    "BG",
    "BF",
    "BI",
    "KH",
    "CM",
    "CA",
    "CV",
    "KY",
    "CF",
    "TD",
    "CL",
    "CN",
    "CX",
    "CC",
    "CO",
    "KM",
    "CG",
    "CD",
    "CK",
    "CR",
    "CI",
    "HR",
    "CU",
    "CW",
    "CY",
    "CZ",
    "DK",
    "DJ",
    "DM",
    "DO",
    "EC",
    "EG",
    "SV",
    "GQ",
    "ER",
    "EE",
    "ET",
    "FK",
    "FO",
    "FJ",
    "FI",
    "FR",
    "GF",
    "PF",
    "TF",
    "GA",
    "GM",
    "GE",
    "DE",
    "GH",
    "GI",
    "GR",
    "GL",
    "GD",
    "GP",
    "GU",
    "GT",
    "GG",
    "GN",
    "GW",
    "GY",
    "HT",
    "HM",
    "VA",
    "HN",
    "HK",
    "HU",
    "IS",
    "IN",
    "ID",
    "IR",
    "IQ",
    "IE",
    "IM",
    "IL",
    "IT",
    "JM",
    "JP",
    "JE",
    "JO",
    "KZ",
    "KE",
    "KI",
    "KP",
    "KR",
    "KW",
    "KG",
    "LA",
    "LV",
    "LB",
    "LS",
    "LR",
    "LY",
    "LI",
    "LT",
    "LU",
    "MO",
    "MK",
    "MG",
    "MW",
    "MY",
    "MV",
    "ML",
    "MT",
    "MH",
    "MQ",
    "MR",
    "MU",
    "YT",
    "MX",
    "FM",
    "MD",
    "MC",
    "MN",
    "ME",
    "MS",
    "MA",
    "MZ",
    "MM",
    "NA",
    "NR",
    "NP",
    "NL",
    "NC",
    "NZ",
    "NI",
    "NE",
    "NG",
    "NU",
    "NF",
    "MP",
    "NO",
    "OM",
    "PK",
    "PW",
    "PS",
    "PA",
    "PG",
    "PY",
    "PE",
    "PH",
    "PN",
    "PL",
    "PT",
    "PR",
    "QA",
    "RE",
    "RO",
    "RU",
    "RW",
    "BL",
    "SH",
    "KN",
    "LC",
    "MF",
    "PM",
    "VC",
    "WS",
    "SM",
    "ST",
    "SA",
    "SN",
    "RS",
    "SC",
    "SL",
    "SG",
    "SX",
    "SK",
    "SI",
    "SB",
    "SO",
    "ZA",
    "GS",
    "ES",
    "LK",
    "SD",
    "SR",
    "SJ",
    "SZ",
    "SE",
    "CH",
    "SY",
    "TW",
    "TJ",
    "TZ",
    "TH",
    "TL",
    "TG",
    "TK",
    "TO",
    "TT",
    "TN",
    "TR",
    "TM",
    "TC",
    "TV",
    "UG",
    "UA",
    "AE",
    "GB",
    "US",
    "UM",
    "UY",
    "UZ",
    "VU",
    "VE",
    "VN",
    "VG",
    "VI",
    "WF",
    "EH",
    "YE",
    "ZM",
    "ZW"
  )

  def initJourney(ukMode: Boolean, callbackUrl: String)(implicit hc: HeaderCarrier): Future[Either[AddressLookupError, String]] = {
    val config = if (ukMode) ukJourneyConfig(callbackUrl) else nonUkJourneyConfig(callbackUrl)
    connector.initJourney(config)
  }

  private def commonOptions(callbackUrl: String): JsObject = Json.obj(
    "continueUrl"                  -> callbackUrl,
    "signOutHref"                  -> appConfig.signOutUrl,
    "showPhaseBanner"              -> true,
    "alphaPhase"                   -> true,
    "useNewGovUkServiceNavigation" -> true,
    "showBackButtons"              -> true,
    "includeHMRCBranding"          -> false,
    "pageHeadingStyle"             -> "govuk-heading-l",
    "timeoutConfig"                -> Json.obj(
      "timeoutAmount"       -> 900,
      "timeoutUrl"          -> controllers.auth.routes.AuthController.signOut().url,
      "timeoutKeepAliveUrl" -> controllers.routes.KeepAliveController.keepAlive().url
    ),
    "confirmPageConfig"        -> Json.obj("showChangeLink" -> true),
    "manualAddressEntryConfig" -> manualAddressEntryConfig
  )

  private def ukJourneyConfig(callbackUrl: String): JsObject =
    Json.obj(
      "version" -> 2,
      "options" -> (commonOptions(callbackUrl) ++ Json.obj(
        "ukMode"           -> true,
        "selectPageConfig" -> Json.obj("showSearchAgainLink" -> false, "showNoneOfTheseOption" -> true)
      )),
      "labels" -> Json.obj(
        "en" -> labelsFor(Lang("en"), uk = true),
        "cy" -> labelsFor(Lang("cy"), uk = true)
      )
    )

  private def nonUkJourneyConfig(callbackUrl: String): JsObject =
    Json.obj(
      "version" -> 2,
      "options" -> (commonOptions(callbackUrl) ++ Json.obj(
        "ukMode"              -> false,
        "allowedCountryCodes" -> notifierAllowedCountryCodes
      )),
      "labels" -> Json.obj(
        "en" -> labelsFor(Lang("en"), uk = false),
        "cy" -> labelsFor(Lang("cy"), uk = false)
      )
    )

  private def manualAddressEntryConfig: JsObject =
    Json.obj(
      "line1MaxLength"         -> 35,
      "line2MaxLength"         -> 35,
      "line3MaxLength"         -> 35,
      "townMaxLength"          -> 35,
      "showOrganisationName"   -> false,
      "mandatoryFields"        -> Json.obj("addressLine1" -> true),
      "maxLengthErrorMessages" -> maxLengthErrorMessages
    )

  private def maxLengthErrorMessages: JsObject = Json.obj(
    "en" -> maxLengthErrorMessagesFor(Lang("en")),
    "cy" -> maxLengthErrorMessagesFor(Lang("cy"))
  )

  private def maxLengthErrorMessagesFor(lang: Lang): JsObject = {
    val messages = messagesApi.preferred(Seq(lang))
    Json.obj(
      "addressLine1" -> messages("addressLookup.error.line1Length"),
      "addressLine2" -> messages("addressLookup.error.line2Length"),
      "addressLine3" -> messages("addressLookup.error.line3Length"),
      "town"         -> messages("addressLookup.error.townLength")
    )
  }

  private def labelsFor(lang: Lang, uk: Boolean): JsObject = {
    implicit val messages: Messages = messagesApi.preferred(Seq(lang))

    val appLevelLabels = Json.obj("navTitle" -> messages("service.name"))

    val editPageLabels = Json.obj(
      "title"         -> messages(if (uk) "addressLookup.uk.edit.title" else "addressLookup.nonUk.edit.title"),
      "heading"       -> messages(if (uk) "addressLookup.uk.edit.heading" else "addressLookup.nonUk.edit.heading"),
      "line1Label"    -> messages("addressLookup.edit.line1Label"),
      "line2Label"    -> messages("addressLookup.edit.line2Label"),
      "line3Label"    -> messages("addressLookup.edit.line3Label"),
      "townLabel"     -> messages("addressLookup.edit.townLabel"),
      "postcodeLabel" -> messages(if (uk) "addressLookup.uk.edit.postcodeLabel" else "addressLookup.nonUk.edit.postcodeLabel"),
      "countryLabel"  -> messages("addressLookup.edit.countryLabel"),
      "submitLabel"   -> messages("site.continue")
    )

    val confirmPageLabels = Json.obj(
      "title"       -> messages("addressLookup.confirm.title"),
      "heading"     -> messages("addressLookup.confirm.heading"),
      "submitLabel" -> messages("site.saveAndContinue")
    )

    val otherLabels =
      if (uk)
        Json.obj("editPage.town.error" -> messages("addressLookup.error.townRequired"))
      else
        Json.obj(
          "editPage.town.error"                   -> messages("addressLookup.error.townRequired"),
          "constants.editPageCountryErrorMessage" -> messages("addressLookup.error.countryRequired")
        )

    if (uk) {
      val lookupPageLabels = Json.obj(
        "title"                 -> messages("addressLookup.uk.lookup.title"),
        "heading"               -> messages("addressLookup.uk.lookup.heading"),
        "postcodeLabel"         -> messages("addressLookup.uk.lookup.postcodeLabel"),
        "submitLabel"           -> messages("addressLookup.uk.lookup.submitLabel"),
        "manualAddressLinkText" -> messages("addressLookup.uk.lookup.manualAddressLinkText")
      )
      val selectPageLabels = Json.obj(
        "title"               -> messages("addressLookup.uk.select.title"),
        "heading"             -> messages("addressLookup.uk.select.heading"),
        "proposalListLabel"   -> messages("addressLookup.uk.select.proposalListLabel"),
        "submitLabel"         -> messages("site.continue"),
        "editAddressLinkText" -> messages("addressLookup.uk.select.editAddressLinkText")
      )
      Json.obj(
        "appLevelLabels"    -> appLevelLabels,
        "lookupPageLabels"  -> lookupPageLabels,
        "selectPageLabels"  -> selectPageLabels,
        "editPageLabels"    -> editPageLabels,
        "confirmPageLabels" -> confirmPageLabels,
        "otherLabels"       -> otherLabels
      )
    } else {
      Json.obj(
        "appLevelLabels" -> appLevelLabels,
        "international"  -> Json.obj(
          "editPageLabels"    -> editPageLabels,
          "confirmPageLabels" -> confirmPageLabels
        ),
        "otherLabels" -> otherLabels
      )
    }
  }
}
