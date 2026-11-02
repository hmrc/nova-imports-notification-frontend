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
import controllers.vehicledetails.routes
import models.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import pages.sections.vehicledetails.*
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import viewmodels.checkAnswers.ConfirmVehicleTypeDetailsHelper
import views.html.ConfirmVehicleTypeDetailsView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*

class ConfirmVehicleTypeDetailsViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: ConfirmVehicleTypeDetailsView = app.injector.instanceOf[ConfirmVehicleTypeDetailsView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private val s      = SupplierNumber(1)
  private val i      = ImportNumber(1)
  private val v      = VehicleNumber(1)
  private val submit = routes.ConfirmVehicleTypeDetailsController.supplierOnSubmit(s, v)

  private val carAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(AddVehicleTypePage(v), AddVehicleType.Car)
    .unsafeSet(AddVehicleDetailsCarPage(v), AddVehicleDetailsCar("Land Rover", "Discovery 4", "3.0TD", "XS", "station wagon"))

  private def supplierDoc(answers: UserAnswers): Document =
    Jsoup.parse(view(ConfirmVehicleTypeDetailsHelper.supplierSummaryList(answers, s, v), submit).toString)

  private def importDocFor(answers: UserAnswers): Document =
    Jsoup.parse(
      view(
        ConfirmVehicleTypeDetailsHelper.importSummaryList(answers, i, v),
        routes.ConfirmVehicleTypeDetailsController.importOnSubmit(i, v)
      ).toString
    )

  private def keys(doc: Document): Seq[String] = doc.select(".govuk-summary-list__key").eachText.asScala.toSeq

  private def valueFor(doc: Document, key: String): String =
    doc
      .select(".govuk-summary-list__row")
      .asScala
      .find(_.select(".govuk-summary-list__key").text == key)
      .value
      .select(".govuk-summary-list__value")
      .text

  private def changeLinkFor(doc: Document, key: String) =
    doc
      .select(".govuk-summary-list__row")
      .asScala
      .find(_.select(".govuk-summary-list__key").text == key)
      .value
      .select(".govuk-summary-list__actions a")

  private def label(field: String): String = msgs(s"confirmVehicleTypeDetails.$field.label")

  "ConfirmVehicleTypeDetailsView" - {

    val doc = supplierDoc(carAnswers)

    "must render the page title, caption and heading" in {
      doc.title must startWith(msgs("confirmVehicleTypeDetails.title"))
      doc.select("span.govuk-caption-l").text mustEqual msgs("confirmVehicleTypeDetails.caption")
      doc.select("h1").text mustEqual msgs("confirmVehicleTypeDetails.heading")
    }

    "must render the Confirm details button posting to the given call" in {
      doc.select("form").attr("action") mustEqual submit.url
      doc.select("form button.govuk-button").text mustEqual msgs("site.confirmDetails")
    }

    "must render the make, model, derivative, trim and body type rows with values and Change links for a car" in {
      keys(doc) mustEqual Seq(label("make"), label("model"), label("derivative"), label("trim"), label("bodyType"))
      valueFor(doc, label("make")) mustEqual "Land Rover"
      valueFor(doc, label("model")) mustEqual "Discovery 4"
      valueFor(doc, label("derivative")) mustEqual "3.0TD"
      valueFor(doc, label("trim")) mustEqual "XS"
      valueFor(doc, label("bodyType")) mustEqual "station wagon"

      Seq("make", "model", "derivative", "trim", "bodyType").foreach { field =>
        val link = changeLinkFor(doc, label(field))
        link.attr("href") mustEqual routes.AddVehicleDetailsCarController.supplierOnPageLoad(s, v, CheckMode).url
        link.select(".govuk-visually-hidden").text mustEqual msgs(s"confirmVehicleTypeDetails.$field.change.hidden")
      }
    }

    "must render the same make/model/derivative/trim/body type rows for a light commercial vehicle, with LCV Change links" in {
      val lcvAnswers = emptyUserAnswers
        .unsafeSet(AddVehicleTypePage(v), AddVehicleType.Lcv)
        .unsafeSet(AddVehicleDetailsLightCommercialPage(v), LightCommercialVehicleDetails("Ford", "Transit", "2.0 TDCi", "L2 H2", "panel van"))
      val lcvDoc = supplierDoc(lcvAnswers)

      keys(lcvDoc) mustEqual Seq(label("make"), label("model"), label("derivative"), label("trim"), label("bodyType"))
      valueFor(lcvDoc, label("bodyType")) mustEqual "panel van"
      changeLinkFor(lcvDoc, label("make")).attr("href") mustEqual
        routes.AddVehicleDetailsLightCommercialController.supplierOnPageLoad(s, v, CheckMode).url
    }

    "must render the hcvType value under Body type and a Cab type row, with no Trim or Derivative rows, for a heavy commercial vehicle" in {
      val hcvAnswers = emptyUserAnswers
        .unsafeSet(AddVehicleTypePage(v), AddVehicleType.Hcv)
        .unsafeSet(
          AddVehicleDetailsHeavyCommercialPage(v),
          AddVehicleDetailsHeavyCommercial("Renault Trucks", "Magnum", "midlift axle tractor", "sleeper cab")
        )
      val hcvDoc = supplierDoc(hcvAnswers)

      keys(hcvDoc) mustEqual Seq(label("make"), label("model"), label("bodyType"), label("cabType"))
      valueFor(hcvDoc, label("bodyType")) mustEqual "midlift axle tractor"
      valueFor(hcvDoc, label("cabType")) mustEqual "sleeper cab"
      changeLinkFor(hcvDoc, label("cabType")).attr("href") mustEqual
        routes.AddVehicleDetailsHeavyCommercialController.supplierOnPageLoad(s, v, CheckMode).url
    }

    "must render all motorcycle rows with human-readable transmission and fuel type text" in {
      val motorcycleAnswers = emptyUserAnswers
        .unsafeSet(AddVehicleTypePage(v), AddVehicleType.Motorcycle)
        .unsafeSet(
          AddVehicleDetailsMotorcyclePage(v),
          AddVehicleDetailsMotorcycle(
            "Honda",
            "GL1200",
            "1200",
            "Gold Wing Deluxe",
            "Road motorcycle",
            "Tourer",
            "Manual",
            MotorcycleFuelType.Petrol,
            "1000"
          )
        )
      val motorcycleDoc = supplierDoc(motorcycleAnswers)

      keys(motorcycleDoc) mustEqual Seq(
        label("make"),
        label("model"),
        label("derivative"),
        label("version"),
        label("bodyType"),
        label("style"),
        label("transmissionType"),
        label("fuelType"),
        label("engineSize")
      )
      valueFor(motorcycleDoc, label("version")) mustEqual "Gold Wing Deluxe"
      valueFor(motorcycleDoc, label("bodyType")) mustEqual "Road motorcycle"
      valueFor(motorcycleDoc, label("style")) mustEqual "Tourer"
      valueFor(motorcycleDoc, label("transmissionType")) mustEqual "Manual"
      valueFor(motorcycleDoc, label("fuelType")) mustEqual msgs("addVehicleDetailsMotorcycle.fuelType.option.petrol")
      valueFor(motorcycleDoc, label("engineSize")) mustEqual "1000"
    }

    "must render the caravan-specific rows for a motor caravan" in {
      val caravanAnswers = emptyUserAnswers
        .unsafeSet(AddVehicleTypePage(v), AddVehicleType.MotorCaravan)
        .unsafeSet(
          AddVehicleDetailsMotorCaravanPage(v),
          AddVehicleDetailsMotorCaravan("Swift", "Coachbuilt", "Voyager", "Voyager 685FB 4 berth", "Fiat", "2.3TD")
        )
      val caravanDoc = supplierDoc(caravanAnswers)

      keys(caravanDoc) mustEqual Seq(
        label("make"),
        label("bodyType"),
        label("modelNameNumber"),
        label("version"),
        label("makeOfBaseVehicle"),
        label("derivative")
      )
      valueFor(caravanDoc, label("make")) mustEqual "Swift"
      valueFor(caravanDoc, label("bodyType")) mustEqual "Coachbuilt"
      valueFor(caravanDoc, label("modelNameNumber")) mustEqual "Voyager"
      valueFor(caravanDoc, label("makeOfBaseVehicle")) mustEqual "Fiat"
    }

    "must render Not provided for the body type when no motor caravan body type was given" in {
      val caravanAnswers = emptyUserAnswers
        .unsafeSet(AddVehicleTypePage(v), AddVehicleType.MotorCaravan)
        .unsafeSet(
          AddVehicleDetailsMotorCaravanPage(v),
          AddVehicleDetailsMotorCaravan("Swift", AddVehicleDetailsMotorCaravan.NoBodyType, "Voyager", "Voyager 685FB 4 berth", "Fiat", "2.3TD")
        )
      val caravanDoc = supplierDoc(caravanAnswers)

      valueFor(caravanDoc, label("bodyType")) mustEqual msgs("confirmVehicleTypeDetails.notProvided")
    }

    "must render the make, model, derivative and brake horsepower rows for an agricultural tractor" in {
      val tractorAnswers = emptyUserAnswers
        .unsafeSet(AddVehicleTypePage(v), AddVehicleType.AgriculturalTractor)
        .unsafeSet(AddVehicleDetailsAgriculturalTractorPage(v), AddVehicleDetailsAgriculturalTractor("John Deere", "SR", "5090M", "200"))
      val tractorDoc = supplierDoc(tractorAnswers)

      keys(tractorDoc) mustEqual Seq(label("make"), label("model"), label("derivative"), label("brakeHorsepower"))
      valueFor(tractorDoc, label("brakeHorsepower")) mustEqual "200"
    }

    "must render only the make, model and derivative rows for a construction, plant or special purpose vehicle" in {
      val plantAnswers = emptyUserAnswers
        .unsafeSet(AddVehicleTypePage(v), AddVehicleType.ContractorsPlant)
        .unsafeSet(AddVehicleDetailsContractorsPlantPage(v), AddVehicleDetailsContractorsPlant("Caterpillar (CAT)", "242B", "Series 3"))
      val plantDoc = supplierDoc(plantAnswers)

      keys(plantDoc) mustEqual Seq(label("make"), label("model"), label("derivative"))
    }

    "must render Change links for the import journey pointing at the import controller" in {
      val importAnswers = emptyUserAnswers
        .unsafeSet(AddVehicleTypePage(v), AddVehicleType.Car)
        .unsafeSet(AddVehicleDetailsCarPage(v), AddVehicleDetailsCar("Land Rover", "Discovery 4", "3.0TD", "XS", "station wagon"))
      val importDoc = importDocFor(importAnswers)

      keys(importDoc) mustEqual Seq(label("make"), label("model"), label("derivative"), label("trim"), label("bodyType"))
      changeLinkFor(importDoc, label("make")).attr("href") mustEqual
        routes.AddVehicleDetailsCarController.importOnPageLoad(i, v, CheckMode).url
    }

    "must render no rows when no vehicle type has been answered" in {
      keys(supplierDoc(emptyUserAnswers)) mustEqual Seq.empty
    }

    "must escape user-entered values" in {
      val escapedAnswers =
        carAnswers.unsafeSet(AddVehicleDetailsCarPage(v), AddVehicleDetailsCar("<b>Land Rover</b>", "Discovery 4", "3.0TD", "XS", "station wagon"))
      val escapedDoc = supplierDoc(escapedAnswers)

      valueFor(escapedDoc, label("make")) mustEqual "<b>Land Rover</b>"
      escapedDoc.select(".govuk-summary-list__value b").size mustEqual 0
    }

    "must render the same content via the render and f methods" in {
      val list = ConfirmVehicleTypeDetailsHelper.supplierSummaryList(carAnswers, s, v)

      view.render(list, submit, request, msgs).toString must include(msgs("confirmVehicleTypeDetails.heading"))
      view.f(list, submit)(request, msgs).toString      must include(msgs("confirmVehicleTypeDetails.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
