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
import models.AddressJourney
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AddressLookupService @Inject() (
  connector: AddressLookupConnector,
  messagesApi: MessagesApi,
  appConfig: FrontendAppConfig
) {

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

  // AVD-S5.1a: the EU plus the United Kingdom.
  // TODO DTR-5976: GB is queried with the BA — ALF routes it back into the UK journey, contradicting the "No" answer.
  private val supplierAllowedCountryCodes: Seq[String] = Seq(
    "AT",
    "BE",
    "BG",
    "HR",
    "CY",
    "CZ",
    "DK",
    "EE",
    "FI",
    "FR",
    "DE",
    "GR",
    "HU",
    "IE",
    "IT",
    "LV",
    "LT",
    "LU",
    "MT",
    "NL",
    "PL",
    "PT",
    "RO",
    "SK",
    "SI",
    "ES",
    "SE",
    "GB"
  )

  private def allowedCountryCodesFor(journey: AddressJourney): Seq[String] = journey match {
    case AddressJourney.Notifier    => notifierAllowedCountryCodes
    case AddressJourney.Supplier(_) => supplierAllowedCountryCodes
    case AddressJourney.Purchaser   => notifierAllowedCountryCodes
  }

  def initJourney(journey: AddressJourney, ukMode: Boolean)(implicit hc: HeaderCarrier): Future[Either[AddressLookupError, String]] = {
    val callbackUrl = appConfig.addressLookupCallbackUrl(journey)
    val config      = if (ukMode) ukJourneyConfig(journey, callbackUrl) else nonUkJourneyConfig(journey, callbackUrl)
    connector.initJourney(config)
  }

  private def commonOptions(callbackUrl: String): JsObject = Json.obj(
    "continueUrl"                  -> callbackUrl,
    "signOutHref"                  -> appConfig.signOutUrl,
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

  private def ukJourneyConfig(journey: AddressJourney, callbackUrl: String): JsObject =
    Json.obj(
      "version" -> 2,
      "options" -> (commonOptions(callbackUrl) ++ Json.obj(
        "ukMode"           -> true,
        "selectPageConfig" -> Json.obj("showSearchAgainLink" -> false, "showNoneOfTheseOption" -> true)
      )),
      "labels" -> Json.obj(
        "en" -> labelsFor(journey, Lang("en"), uk = true),
        "cy" -> labelsFor(journey, Lang("cy"), uk = true)
      )
    )

  private def nonUkJourneyConfig(journey: AddressJourney, callbackUrl: String): JsObject =
    Json.obj(
      "version" -> 2,
      "options" -> (commonOptions(callbackUrl) ++ Json.obj(
        "ukMode"              -> false,
        "allowedCountryCodes" -> allowedCountryCodesFor(journey)
      )),
      "labels" -> Json.obj(
        "en" -> labelsFor(journey, Lang("en"), uk = false),
        "cy" -> labelsFor(journey, Lang("cy"), uk = false)
      )
    )

  private def manualAddressEntryConfig: JsObject =
    Json.obj(
      "line1MaxLength"         -> 35,
      "line2MaxLength"         -> 35,
      "line3MaxLength"         -> 35,
      "townMaxLength"          -> 35,
      "showOrganisationName"   -> false,
      "mandatoryFields"        -> Json.obj("addressLine1" -> true, "addressLine2" -> true),
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

  // A journey only defines its own label keys where the copy differs; everything else falls back to the shared key.
  private def label(journey: AddressJourney, key: String)(implicit messages: Messages): String = {
    val scoped = s"addressLookup.${journey.keySegment}.$key"
    if (messages.isDefinedAt(scoped)) messages(scoped) else messages(s"addressLookup.$key")
  }

  private def labelsFor(journey: AddressJourney, lang: Lang, uk: Boolean): JsObject = {
    implicit val messages: Messages = messagesApi.preferred(Seq(lang))

    def alf(key: String): String = label(journey, key)

    val appLevelLabels = Json.obj("navTitle" -> messages("service.name"))

    val editPageLabels = Json.obj(
      "title"         -> alf(if (uk) "uk.edit.title" else "nonUk.edit.title"),
      "heading"       -> alf(if (uk) "uk.edit.heading" else "nonUk.edit.heading"),
      "line1Label"    -> alf("edit.line1Label"),
      "line2Label"    -> alf("edit.line2Label"),
      "line3Label"    -> alf("edit.line3Label"),
      "townLabel"     -> alf("edit.townLabel"),
      "postcodeLabel" -> alf(if (uk) "uk.edit.postcodeLabel" else "nonUk.edit.postcodeLabel"),
      "countryLabel"  -> alf("edit.countryLabel"),
      "submitLabel"   -> messages("site.continue")
    )

    val confirmPageLabels = Json.obj(
      "title"       -> alf("confirm.title"),
      "heading"     -> alf("confirm.heading"),
      "submitLabel" -> alf("confirm.submitLabel")
    )

    val mandatoryFieldErrorLabels = Json.obj(
      "editPage.line1.error" -> alf("error.line1Required"),
      "editPage.line2.error" -> alf("error.line2Required"),
      "editPage.town.error"  -> alf("error.townRequired")
    )

    val otherLabels =
      if (uk)
        mandatoryFieldErrorLabels
      else
        mandatoryFieldErrorLabels ++ Json.obj(
          "constants.editPageCountryErrorMessage"          -> alf("error.countryRequired"),
          "constants.countryPickerPageCountryErrorMessage" -> alf("error.countryPickerRequired")
        )

    if (uk) {
      val lookupPageLabels = Json.obj(
        "title"                 -> alf("uk.lookup.title"),
        "heading"               -> alf("uk.lookup.heading"),
        "postcodeLabel"         -> alf("uk.lookup.postcodeLabel"),
        "submitLabel"           -> alf("uk.lookup.submitLabel"),
        "manualAddressLinkText" -> alf("uk.lookup.manualAddressLinkText")
      )
      val selectPageLabels = Json.obj(
        "title"               -> alf("uk.select.title"),
        "heading"             -> alf("uk.select.heading"),
        "proposalListLabel"   -> alf("uk.select.proposalListLabel"),
        "submitLabel"         -> messages("site.continue"),
        "editAddressLinkText" -> alf("uk.select.editAddressLinkText")
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
      // AYA1.1a / AVD-S5.1a country picker (ALF): the H1 already names the country question, so the dropdown's own
      // label is visually redundant. Hide it visually but keep it as the accessible name for the <select>
      // (targetID="countryCode") to avoid an empty accessible name.
      val countryPickerLabels = Json.obj(
        "title"        -> alf("countryPicker.title"),
        "heading"      -> alf("countryPicker.heading"),
        "countryLabel" -> s"""<span class="govuk-visually-hidden">${alf("countryPicker.countryLabel")}</span>"""
      )

      Json.obj(
        "appLevelLabels"      -> appLevelLabels,
        "countryPickerLabels" -> countryPickerLabels,
        "international"       -> Json.obj(
          "editPageLabels"    -> editPageLabels,
          "confirmPageLabels" -> confirmPageLabels
        ),
        "otherLabels" -> otherLabels
      )
    }
  }
}
