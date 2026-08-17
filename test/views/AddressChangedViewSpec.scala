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
import models.{Address, Country, NormalMode, SupplierNumber}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.AddressChangedView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AddressChangedViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: AddressChangedView = app.injector.instanceOf[AddressChangedView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private val ukAddress = Address(
    lines = Seq("12 High Street", "Reading"),
    postcode = Some("RE12 9GC"),
    country = Country("GB", "United Kingdom")
  )

  private val nonUkAddress = Address(
    lines = Seq("Musterstrasse 12", "Berlin"),
    postcode = Some("10115"),
    country = Country("DE", "Germany")
  )

  private val supplierNumber = SupplierNumber(1)

  private def notifierHtml(address: Address) =
    view(
      address,
      "addressChanged",
      routes.AddressChangedController.onChangeAddress(),
      routes.AddressChangedController.onSubmit()
    )(request, msgs).toString

  private def supplierHtml(address: Address) =
    view(
      address,
      "supplierAddressChanged",
      routes.AddressChangedController.supplierOnChangeAddress(supplierNumber),
      routes.AddressChangedController.supplierOnSubmit(supplierNumber)
    )(request, msgs).toString

  "AddressChangedView" - {

    "must render every address line" in {
      val html = notifierHtml(ukAddress)
      html must include("12 High Street")
      html must include("Reading")
    }

    // AYA3.0 / AVD-S7.0 both specify: postcode for a UK address, country for a non-UK one
    "must show the postcode and not the country for a UK address" in {
      val html = notifierHtml(ukAddress)
      html must include("RE12 9GC")
      html must not include "United Kingdom"
    }

    "must show the country and not the postcode for a non-UK address" in {
      val html = notifierHtml(nonUkAddress)
      html must include("Germany")
      html must not include "10115"
    }

    "must render the notifier copy and routes when given the notifier key prefix" in {
      val html = notifierHtml(ukAddress)
      html must include(msgs("addressChanged.heading"))
      html must include(msgs("addressChanged.saveAndContinue"))
      html must include(routes.AddressChangedController.onChangeAddress().url)
      html must include(routes.AddressChangedController.onSubmit().url)
    }

    "must render the supplier copy and routes when given the supplier key prefix" in {
      val html = supplierHtml(ukAddress)
      html must include(msgs("supplierAddressChanged.heading"))
      html must include(msgs("supplierAddressChanged.saveAndContinue"))
      html must include(routes.AddressChangedController.supplierOnChangeAddress(supplierNumber).url)
      html must include(routes.AddressChangedController.supplierOnSubmit(supplierNumber).url)
    }

    "must use Confirm address as the supplier button, not Save and continue" in {
      supplierHtml(ukAddress) must include("Confirm address")
    }
  }
}
