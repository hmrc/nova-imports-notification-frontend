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

package controllers.vehicledetails

import base.SpecBase
import config.FrontendAppConfig
import connectors.{GetDraftNotificationError, NovaImportsBackendConnector, UpdateSectionError}
import controllers.{routes, vehicledetails}
import models.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.supplierdetails.IsSupplierVatRegisteredPage
import pages.sections.vehicledetails.*
import pages.{DraftIdPage, DraftVersionIdPage, ValidatedVinListPage}
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

class ConfirmAdditionalVehicleDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val s     = SupplierNumber(1)
  private val i     = ImportNumber(1)
  private val v     = VehicleNumber(1)
  private val draft = DraftId("DRAFT-001")

  private lazy val supplierRoute       = vehicledetails.routes.ConfirmAdditionalVehicleDetailsController.supplierOnPageLoad(s, v).url
  private lazy val importRoute         = vehicledetails.routes.ConfirmAdditionalVehicleDetailsController.importOnPageLoad(i, v).url
  private lazy val supplierSubmitRoute = vehicledetails.routes.ConfirmAdditionalVehicleDetailsController.supplierOnSubmit(s, v).url
  private lazy val importSubmitRoute   = vehicledetails.routes.ConfirmAdditionalVehicleDetailsController.importOnSubmit(i, v).url

  private val supplierSectionId = "supplier/1/vehicle/1/additional-information"
  private val importSectionId   = "import/1/vehicle/1/additional-information"

  private val supplierOneVehicle: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, draft)
    .unsafeSet(DraftVersionIdPage, 3L)
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AmendSubmittedNotificationPage, false)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
    .unsafeSet(IsSupplierVatRegisteredPage(s), true)
    .unsafeSet(NumberOfVehiclesPage(v), NumberOfVehicles(OneMultiVehicle.One, None))
    .unsafeSet(VehicleIdentificationNumberPage(v), "1ABCD2E03FG456789")
    .unsafeSet(VehicleRegistrationNumberKnownPage(v), false)
    .unsafeSet(MileagePage(v), Mileage(MileageUnit.MILES, "25000"))

  private val supplierMultipleVehicles: UserAnswers = supplierOneVehicle
    .unsafeSet(NumberOfVehiclesPage(v), NumberOfVehicles(OneMultiVehicle.More, Some(2)))
    .unsafeSet(ValidatedVinListPage, Seq("VIN00000000000001", "VIN00000000000002"))

  private val importOneVehicle: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, draft)
    .unsafeSet(DraftVersionIdPage, 3L)
    .unsafeSet(VehicleFromEuPage, false)
    .unsafeSet(AmendSubmittedNotificationPage, false)
    .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A")))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1)))
    .unsafeSet(NumberOfVehiclesPage(v), NumberOfVehicles(OneMultiVehicle.One, None))
    .unsafeSet(VehicleIdentificationNumberPage(v), "1ABCD2E03FG456789")
    .unsafeSet(MileagePage(v), Mileage(MileageUnit.KM, "1200"))
    .unsafeSet(TotalAmountPaidPage(v), "15000")
    .unsafeSet(PaymentCurrencyPage(v), "EUR")

  private val supplierAllAnswers: UserAnswers = supplierOneVehicle
    .unsafeSet(VehicleRegistrationNumberKnownPage(v), true)
    .unsafeSet(VehicleRegistrationNumberPage(v), "AB12 CDE")
    .unsafeSet(DateOfArrivalPage(v), LocalDate.of(2026, 3, 27))
    .unsafeSet(LeftOrRightHandDrivePage(v), LeftOrRightHandDrive.LHD)
    .unsafeSet(TotalValueOfOptionsPage(v), "1200")
    .unsafeSet(CouldNotClaimVatPage(v), true)
    .unsafeSet(VatMarginSchemePage(v), false)

  private val importAllAnswers: UserAnswers = importOneVehicle
    .unsafeSet(DateOfArrivalPage(v), LocalDate.of(2026, 3, 27))
    .unsafeSet(CommodityCodePage(v), "8703123456")
    .unsafeSet(LeftOrRightHandDrivePage(v), LeftOrRightHandDrive.RHD)

  private def draftWith(sections: (String, JsObject)*): DraftNotification =
    DraftNotification(
      draftId = draft.value,
      createdDate = "2026-11-06",
      lastUpdatedDate = None,
      sections = sections.map { case (id, data) => id -> DraftNotificationSection(Some(data)) }.toMap
    )

  private def stubSessionRepository(answers: UserAnswers): SessionRepository = {
    val repository = mock[SessionRepository]
    when(repository.setPage(any(), any(), any())(any())).thenReturn(Future.successful(answers))
    repository
  }

  private def connectorReturning(draftNotification: DraftNotification, versions: Long*): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getDraftNotification(any())(any[HeaderCarrier])).thenReturn(Future.successful(Right(draftNotification)))
    versions.toList match {
      case first :: rest =>
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(first)), rest.map(v => Future.successful(Right(v)))*)
      case Nil => ()
    }
    connector
  }

  private def application(
    answers: Option[UserAnswers],
    connector: NovaImportsBackendConnector = mock[NovaImportsBackendConnector],
    repository: SessionRepository = mock[SessionRepository]
  ): Application =
    applicationBuilder(answers)
      .overrides(
        bind[NovaImportsBackendConnector].toInstance(connector),
        bind[SessionRepository].toInstance(repository)
      )
      .build()

  private def page(app: Application, url: String): Document =
    running(app) {
      val result = route(app, FakeRequest(GET, url)).value
      status(result) mustEqual OK
      Jsoup.parse(contentAsString(result))
    }

  private def rows(doc: Document): Seq[(String, String)] =
    doc.select(".govuk-summary-list__row").asScala.toSeq.map { row =>
      row.select(".govuk-summary-list__key").text() -> row.select(".govuk-summary-list__value").text()
    }

  private def changeLinks(doc: Document): Seq[(String, String)] =
    doc.select(".govuk-summary-list__actions a").asScala.toSeq.map(a => a.select(".govuk-visually-hidden").text() -> a.attr("href"))

  private def capturedBodies(connector: NovaImportsBackendConnector, times: Int): Seq[(String, JsObject)] = {
    val ids    = ArgumentCaptor.forClass(classOf[String])
    val bodies = ArgumentCaptor.forClass(classOf[JsObject])
    verify(connector, org.mockito.Mockito.times(times)).updateDraftSection(eqTo(draft), ids.capture(), bodies.capture())(any[HeaderCarrier])
    ids.getAllValues.asScala.toSeq.zip(bodies.getAllValues.asScala.toSeq)
  }

  "ConfirmAdditionalVehicleDetailsController" - {

    "supplierOnPageLoad" - {

      "must show the caption, heading, one-vehicle rows with change links, and a Confirm details button posting to the supplier submit" in {
        val app = application(Some(supplierOneVehicle))
        val doc = page(app, supplierRoute)
        val m   = messages(app)

        doc.title must startWith(m("confirmAdditionalVehicleDetails.title"))
        doc.select(".govuk-caption-l").text() mustEqual m("confirmAdditionalVehicleDetails.caption")
        doc.select("h1").text() mustEqual m("confirmAdditionalVehicleDetails.heading")
        doc.select("form").attr("action") mustEqual supplierSubmitRoute
        doc.select("form button").text() mustEqual m("site.confirmDetails")

        rows(doc) mustEqual Seq(
          "How many vehicles is this notification for?"          -> "One vehicle",
          "Vehicle identification number"                        -> "1ABCD2E03FG456789",
          "Do you know the current vehicle registration number?" -> "No",
          "Mileage"                                              -> "25000 Miles (mi)"
        )

        changeLinks(doc) mustEqual Seq(
          "how many vehicles this notification is for" -> vehicledetails.routes.NumberOfVehiclesController.supplierOnPageLoad(s, v, CheckMode).url,
          "vehicle identification number" -> vehicledetails.routes.VehicleIdentificationNumberController.supplierOnPageLoad(s, v, CheckMode).url,
          "whether you know the current vehicle identification number" ->
            vehicledetails.routes.VehicleRegistrationNumberKnownController.onPageLoad(s, v, CheckMode).url,
          "mileage" -> vehicledetails.routes.MileageController.supplierOnPageLoad(s, v, CheckMode).url
        )
      }

      "must show the notification reference first when the notification is an amendment" in {
        val answers = supplierOneVehicle
          .unsafeSet(AmendSubmittedNotificationPage, true)
          .unsafeSet(NotificationReferencePage(v), "NOVA26E123456")
        val doc = page(application(Some(answers)), supplierRoute)

        rows(doc).head mustEqual ("Notification reference" -> "NOVA26E123456")
        changeLinks(doc).head mustEqual
          ("notification reference" -> vehicledetails.routes.NotificationReferenceController.supplierOnPageLoad(s, v, CheckMode).url)
      }

      "must not show the notification reference when the notification is not an amendment" in {
        val answers = supplierOneVehicle.unsafeSet(NotificationReferencePage(v), "NOVA26E123456")
        rows(page(application(Some(answers)), supplierRoute)).map(_._1) must not contain "Notification reference"
      }

      "must list every VIN and show no change links when more than one vehicle was selected on AVD10.0" in {
        val doc = page(application(Some(supplierMultipleVehicles)), supplierRoute)

        rows(doc) mustEqual Seq(
          "How many vehicles is this notification for?" -> "More than one vehicle",
          "Vehicle identification numbers"              -> "VIN00000000000001 VIN00000000000002",
          "Mileage"                                     -> "25000 Miles (mi)"
        )
        doc.select(".govuk-summary-list__row").get(1).select(".govuk-summary-list__value br").size mustEqual 1
        changeLinks(doc) mustBe empty
      }

      "must show every answered additional vehicle detail in order with supplier change links" in {
        val doc = page(application(Some(supplierAllAnswers)), supplierRoute)

        rows(doc) mustEqual Seq(
          "How many vehicles is this notification for?"                        -> "One vehicle",
          "Vehicle identification number"                                      -> "1ABCD2E03FG456789",
          "Do you know the current vehicle registration number?"               -> "Yes",
          "Current vehicle registration number"                                -> "AB12 CDE",
          "When did the vehicle arrive in the UK?"                             -> "27/03/2026",
          "Mileage"                                                            -> "25000 Miles (mi)",
          "Is the vehicle left-hand drive or right-hand drive?"                -> "Left-hand drive",
          "Total value of options"                                             -> "1200",
          "Was the vehicle supplied by a business that could not reclaim VAT?" -> "Yes",
          "Was the vehicle sold to you under the VAT margin scheme?"           -> "No"
        )

        changeLinks(doc).drop(3) mustEqual Seq(
          "current vehicle identification number" -> vehicledetails.routes.VehicleRegistrationNumberController.onPageLoad(s, v, CheckMode).url,
          "when the vehicle arrived in the UK"    -> vehicledetails.routes.DateOfArrivalController.supplierOnPageLoad(s, v, CheckMode).url,
          "mileage"                               -> vehicledetails.routes.MileageController.supplierOnPageLoad(s, v, CheckMode).url,
          "whether the vehicle is left-hand drive or right-hand drive" ->
            vehicledetails.routes.LeftOrRightHandDriveController.supplierOnPageLoad(s, v, CheckMode).url,
          "total value of options for the vehicle" -> vehicledetails.routes.TotalValueOfOptionsController.onPageLoad(s, v, CheckMode).url,
          "whether the vehicle was supplied by a business that could not reclaim VAT" ->
            vehicledetails.routes.CouldNotClaimVatController.onPageLoad(s, v, CheckMode).url,
          "whether the vehicle was sold to you under the VAT margin scheme" ->
            vehicledetails.routes.VatMarginSchemeController.onPageLoad(s, v, CheckMode).url
        )
      }

      "must not show the current registration number when the answer to AVD10.2 is No" in {
        val answers = supplierAllAnswers.unsafeSet(VehicleRegistrationNumberKnownPage(v), false)
        rows(page(application(Some(answers)), supplierRoute)).map(_._1) must not contain "Current vehicle registration number"
      }

      "must not show the VAT reclaim or margin scheme rows when the supplier is not VAT registered" in {
        val answers = supplierAllAnswers.unsafeSet(IsSupplierVatRegisteredPage(s), false)
        rows(page(application(Some(answers)), supplierRoute)).map(_._1) must contain noneOf (
          "Was the vehicle supplied by a business that could not reclaim VAT?",
          "Was the vehicle sold to you under the VAT margin scheme?"
        )
      }

      "must not show the total value of options when the vehicle was not brought from the EU" in {
        val answers = supplierAllAnswers.unsafeSet(VehicleFromEuPage, false)
        rows(page(application(Some(answers)), supplierRoute)).map(_._1) must not contain "Total value of options"
      }

      "must not show the total amount paid or currency when the vehicle was brought from the EU" in {
        val answers = supplierOneVehicle.unsafeSet(TotalAmountPaidPage(v), "15000").unsafeSet(PaymentCurrencyPage(v), "EUR")
        rows(page(application(Some(answers)), supplierRoute)).map(_._1) must contain noneOf ("Total amount paid", "Currency")
      }
    }

    "importOnPageLoad" - {

      "must show the import rows, including total amount paid and currency, with import change links" in {
        val app        = application(Some(importOneVehicle))
        val doc        = page(app, importRoute)
        val currencies = app.injector.instanceOf[FrontendAppConfig].currencies
        val euro       = currencies.find(_.code == "EUR").value.displayName

        doc.select("form").attr("action") mustEqual importSubmitRoute
        rows(doc) mustEqual Seq(
          "How many vehicles is this notification for?" -> "One vehicle",
          "Vehicle identification number"               -> "1ABCD2E03FG456789",
          "When did the vehicle arrive in the UK?"      -> "Not provided",
          "Mileage"                                     -> "1200 Kilometres (km)",
          "Total amount paid"                           -> "15000",
          "Currency"                                    -> euro
        )
        changeLinks(doc).map(_._2) mustEqual Seq(
          vehicledetails.routes.NumberOfVehiclesController.importOnPageLoad(i, v, CheckMode).url,
          vehicledetails.routes.VehicleIdentificationNumberController.importOnPageLoad(i, v, CheckMode).url,
          vehicledetails.routes.DateOfArrivalController.importOnPageLoad(i, v, CheckMode).url,
          vehicledetails.routes.MileageController.importOnPageLoad(i, v, CheckMode).url,
          vehicledetails.routes.TotalAmountPaidController.onPageLoadImport(i, v, CheckMode).url,
          vehicledetails.routes.PaymentCurrencyController.importOnPageLoad(i, v, CheckMode).url
        )
      }

      "must show the date of arrival, commodity code and drive side with import change links" in {
        val app  = application(Some(importAllAnswers))
        val doc  = page(app, importRoute)
        val euro = app.injector.instanceOf[FrontendAppConfig].currencies.find(_.code == "EUR").value.displayName

        rows(doc) mustEqual Seq(
          "How many vehicles is this notification for?"         -> "One vehicle",
          "Vehicle identification number"                       -> "1ABCD2E03FG456789",
          "When did the vehicle arrive in the UK?"              -> "27/03/2026",
          "Commodity code"                                      -> "8703123456",
          "Mileage"                                             -> "1200 Kilometres (km)",
          "Is the vehicle left-hand drive or right-hand drive?" -> "Right-hand drive",
          "Total amount paid"                                   -> "15000",
          "Currency"                                            -> euro
        )

        changeLinks(doc).map(_._2).slice(2, 5) mustEqual Seq(
          vehicledetails.routes.DateOfArrivalController.importOnPageLoad(i, v, CheckMode).url,
          vehicledetails.routes.CommodityCodeController.onPageLoad(i, v, CheckMode).url,
          vehicledetails.routes.MileageController.importOnPageLoad(i, v, CheckMode).url
        )
        changeLinks(doc)(5) mustEqual
          ("whether the vehicle is left-hand drive or right-hand drive" ->
            vehicledetails.routes.LeftOrRightHandDriveController.importOnPageLoad(i, v, CheckMode).url)
      }
    }

    "data guard" - {

      "must redirect to ERR1.0 on GET and POST when there is no draftId" in {
        val answers = supplierOneVehicle.remove(DraftIdPage).success.value
        Seq(GET -> supplierRoute, POST -> supplierSubmitRoute, GET -> importRoute, POST -> importSubmitRoute).foreach { case (method, url) =>
          val app = application(Some(answers))
          running(app) {
            val result = route(app, FakeRequest(method, url)).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
          }
        }
      }

      "must allow access with only a draftId defined" in {
        val answers = emptyUserAnswers.unsafeSet(DraftIdPage, draft)
        page(application(Some(answers)), supplierRoute).select("h1").text() mustEqual "Check the additional vehicle details before you continue"
      }
    }

    "supplierOnSubmit" - {

      "must call F4 once for one vehicle, keeping unrelated formp keys and replacing the ones this page owns, then save the new versionId" in {
        val existing = Json.obj(
          "euMemberStates"        -> "AT,BE",
          "areYouClaimingRelief"  -> false,
          "mileage"               -> "1",
          "mileageUnits"          -> "KM",
          "notificationReference" -> "NOVA26E000000"
        )
        val connector  = connectorReturning(draftWith(supplierSectionId -> existing), 4L)
        val repository = stubSessionRepository(supplierOneVehicle)
        val app        = application(Some(supplierOneVehicle), connector, repository)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierSubmitRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url

          capturedBodies(connector, 1) mustEqual Seq(
            supplierSectionId -> Json.obj(
              "euMemberStates"          -> "AT,BE",
              "areYouClaimingRelief"    -> false,
              "isAmendment"             -> false,
              "oneOrMultipleVehicls"    -> "One",
              "vehicleIdNumber"         -> "1ABCD2E03FG456789",
              "confirmVehicleIdNumber"  -> "1ABCD2E03FG456789",
              "mileage"                 -> "25000",
              "mileageUnits"            -> "MILES",
              "isSupplierVatRegistered" -> true,
              "versionId"               -> 3L
            )
          )
          verify(repository).setPage(any(), eqTo(DraftVersionIdPage), eqTo(4L))(any())
        }
      }

      "must send every additional vehicle detail answered for a VAT registered supplier" in {
        val connector = connectorReturning(draftWith(), 4L)
        val app       = application(Some(supplierAllAnswers), connector, stubSessionRepository(supplierAllAnswers))

        running(app) {
          route(app, FakeRequest(POST, supplierSubmitRoute)).value.futureValue

          capturedBodies(connector, 1) mustEqual Seq(
            supplierSectionId -> Json.obj(
              "isAmendment"                  -> false,
              "oneOrMultipleVehicls"         -> "One",
              "vehicleIdNumber"              -> "1ABCD2E03FG456789",
              "confirmVehicleIdNumber"       -> "1ABCD2E03FG456789",
              "currentRegistrationNumber"    -> "AB12 CDE",
              "dateArrivedInUk"              -> "27/03/2026",
              "mileage"                      -> "25000",
              "mileageUnits"                 -> "MILES",
              "leftOrRightHand"              -> "LHD",
              "totalValueOfOptions"          -> "1200",
              "isSupplierVatRegistered"      -> true,
              "businessUnableToReclaimVat"   -> true,
              "vehicleSoldUnderMarginScheme" -> false,
              "versionId"                    -> 3L
            )
          )
        }
      }

      "must drop a stale registration number, VAT reclaim and margin scheme answer when they no longer apply" in {
        val answers = supplierAllAnswers
          .unsafeSet(VehicleRegistrationNumberKnownPage(v), false)
          .unsafeSet(IsSupplierVatRegisteredPage(s), false)
        val existing = Json.obj(
          "currentRegistrationNumber"    -> "OLD 123",
          "businessUnableToReclaimVat"   -> true,
          "vehicleSoldUnderMarginScheme" -> true
        )
        val connector = connectorReturning(draftWith(supplierSectionId -> existing), 4L)
        val app       = application(Some(answers), connector, stubSessionRepository(answers))

        running(app) {
          route(app, FakeRequest(POST, supplierSubmitRoute)).value.futureValue

          val body = capturedBodies(connector, 1).head._2
          body.keys must contain noneOf ("currentRegistrationNumber", "businessUnableToReclaimVat", "vehicleSoldUnderMarginScheme")
          (body \ "isSupplierVatRegistered").as[Boolean] mustBe false
        }
      }

      "must send the notification reference when the notification is an amendment" in {
        val answers = supplierOneVehicle
          .unsafeSet(AmendSubmittedNotificationPage, true)
          .unsafeSet(NotificationReferencePage(v), "NOVA26E123456")
        val connector = connectorReturning(draftWith(), 4L)
        val app       = application(Some(answers), connector, stubSessionRepository(answers))

        running(app) {
          status(route(app, FakeRequest(POST, supplierSubmitRoute)).value) mustEqual SEE_OTHER
          val body = capturedBodies(connector, 1).head._2
          (body \ "isAmendment").as[Boolean] mustBe true
          (body \ "notificationReference").as[String] mustEqual "NOVA26E123456"
        }
      }

      "must call F4 for every multiplied vehicle in turn, keeping each VIN and passing on the versionId from the previous call" in {
        def multiplied(vin: String) =
          Json.obj("vehicleIdNumber" -> vin, "confirmVehicleIdNumber" -> vin, "oneOrMultipleVehicls" -> "Multiple")
        val draftNotification = draftWith(
          "supplier/1/vehicle/2/additional-information" -> multiplied("VIN00000000000002"),
          "supplier/1/vehicle/1/additional-information" -> multiplied("VIN00000000000001"),
          "supplier/2/vehicle/1/additional-information" -> multiplied("OTHERSUPPLIER0001")
        )
        val connector  = connectorReturning(draftNotification, 4L, 5L)
        val repository = stubSessionRepository(supplierMultipleVehicles)
        val app        = application(Some(supplierMultipleVehicles), connector, repository)

        running(app) {
          status(route(app, FakeRequest(POST, supplierSubmitRoute)).value) mustEqual SEE_OTHER

          val calls = capturedBodies(connector, 2)
          calls.map(_._1) mustEqual Seq("supplier/1/vehicle/1/additional-information", "supplier/1/vehicle/2/additional-information")
          calls.map { case (_, body) => (body \ "vehicleIdNumber").as[String] } mustEqual Seq("VIN00000000000001", "VIN00000000000002")
          calls.map { case (_, body) => (body \ "versionId").as[Long] } mustEqual Seq(3L, 4L)
          calls.foreach { case (_, body) =>
            (body \ "oneOrMultipleVehicls").as[String] mustEqual "Multiple"
            (body \ "mileage").as[String] mustEqual "25000"
          }
          verify(repository).setPage(any(), eqTo(DraftVersionIdPage), eqTo(5L))(any())
        }
      }

      "must stop at the first F4 failure and route to the There is a problem page" in {
        def multiplied(vin: String) = Json.obj("vehicleIdNumber" -> vin, "oneOrMultipleVehicls" -> "Multiple")
        val connector               = mock[NovaImportsBackendConnector]
        when(connector.getDraftNotification(any())(any[HeaderCarrier])).thenReturn(
          Future.successful(
            Right(
              draftWith(
                "supplier/1/vehicle/1/additional-information" -> multiplied("VIN00000000000001"),
                "supplier/1/vehicle/2/additional-information" -> multiplied("VIN00000000000002")
              )
            )
          )
        )
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(400, "bad"))))
        val repository = mock[SessionRepository]
        val app        = application(Some(supplierMultipleVehicles), connector, repository)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierSubmitRoute)).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          capturedBodies(connector, 1)
          verify(repository, never()).setPage(any(), any(), any())(any())
        }
      }

      "must route to the There is a problem page when the draft cannot be retrieved" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.getDraftNotification(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(GetDraftNotificationError.NotFound)))
        val app = application(Some(supplierOneVehicle), connector)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierSubmitRoute)).value
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(connector, never()).updateDraftSection(any(), any(), any())(any[HeaderCarrier])
        }
      }

      "must route to the There is a problem page when more than one vehicle was selected but no multiplied vehicles are in formp" in {
        val connector = connectorReturning(draftWith(supplierSectionId -> Json.obj("vehicleIdNumber" -> "X")))
        val app       = application(Some(supplierMultipleVehicles), connector)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierSubmitRoute)).value
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(connector, never()).updateDraftSection(any(), any(), any())(any[HeaderCarrier])
        }
      }

      "must route to the There is a problem page without calling the backend when the number of vehicles is missing" in {
        val connector = mock[NovaImportsBackendConnector]
        val answers   = supplierOneVehicle.remove(NumberOfVehiclesPage(v)).success.value
        val app       = application(Some(answers), connector)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierSubmitRoute)).value
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(connector, never()).getDraftNotification(any())(any[HeaderCarrier])
        }
      }
    }

    "importOnSubmit" - {

      "must send the date of arrival, commodity code and drive side for an import vehicle" in {
        val connector = connectorReturning(draftWith(), 4L)
        val app       = application(Some(importAllAnswers), connector, stubSessionRepository(importAllAnswers))

        running(app) {
          route(app, FakeRequest(POST, importSubmitRoute)).value.futureValue

          val body = capturedBodies(connector, 1).head._2
          (body \ "dateArrivedInUk").as[String] mustEqual "27/03/2026"
          (body \ "commodityCode").as[String] mustEqual "8703123456"
          (body \ "leftOrRightHand").as[String] mustEqual "RHD"
          body.keys must contain noneOf ("totalValueOfOptions", "businessUnableToReclaimVat", "vehicleSoldUnderMarginScheme")
        }
      }

      "must call F4 on the import section with the total amount paid and currency" in {
        val connector = connectorReturning(draftWith(), 4L)
        val app       = application(Some(importOneVehicle), connector, stubSessionRepository(importOneVehicle))

        running(app) {
          val result = route(app, FakeRequest(POST, importSubmitRoute)).value
          redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url

          capturedBodies(connector, 1) mustEqual Seq(
            importSectionId -> Json.obj(
              "isAmendment"              -> false,
              "oneOrMultipleVehicls"     -> "One",
              "vehicleIdNumber"          -> "1ABCD2E03FG456789",
              "confirmVehicleIdNumber"   -> "1ABCD2E03FG456789",
              "mileage"                  -> "1200",
              "mileageUnits"             -> "KM",
              "pricePaidForVehicleEntry" -> "15000",
              "currencyUsed"             -> "EUR",
              "versionId"                -> 3L
            )
          )
        }
      }
    }
  }
}
