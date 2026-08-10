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
import models.{Address, Country, SupplierNumber}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.SupplierAddressChangedView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SupplierAddressChangedViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: SupplierAddressChangedView = app.injector.instanceOf[SupplierAddressChangedView]

  private val supplierNumber = SupplierNumber(1)

  private val ukAddress = Address(
    lines = Seq("23 North Road", "East London", "London"),
    postcode = Some("ER45 6UI"),
    country = Country("GB", "United Kingdom")
  )

  private val nonUkAddress = Address(
    lines = Seq("Musterstrasse 12", "Berlin"),
    postcode = None,
    country = Country("DE", "Germany")
  )

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "SupplierAddressChangedView" - {

    "must render the correct heading" in {
      val html: String = view(ukAddress, supplierNumber)(request, msgs).toString

      html must include(msgs("supplierAddressChanged.heading"))
    }

    "must render the correct page title" in {
      val html: String = view(ukAddress, supplierNumber)(request, msgs).toString

      html must include(msgs("supplierAddressChanged.title"))
    }

    "must render the 'Add vehicle details' caption" in {
      val html: String = view(ukAddress, supplierNumber)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("supplierAddressChanged.caption"))
    }

    "must render the explanatory body and check-address subheading" in {
      val html: String = view(ukAddress, supplierNumber)(request, msgs).toString

      html must include(msgs("supplierAddressChanged.body"))
      html must include(msgs("supplierAddressChanged.checkHeading"))
    }

    "must render each address line, the postcode and the country for a UK address" in {
      val html: String = view(ukAddress, supplierNumber)(request, msgs).toString

      html must include("23 North Road")
      html must include("East London")
      html must include("London")
      html must include("ER45 6UI")
      html must include("United Kingdom")
    }

    "must not render a postcode for a non-UK address" in {
      val html: String = view(nonUkAddress, supplierNumber)(request, msgs).toString

      html must include("Musterstrasse 12")
      html must include("Berlin")
      html must include("Germany")
      html must not include "ER45 6UI"
    }

    "must render the Change address link pointing at the AVD-S5.0 URL for the supplier" in {
      val html: String = view(ukAddress, supplierNumber)(request, msgs).toString

      html must include(msgs("supplierAddressChanged.changeAddress"))
      html must include("/nova-imports/supplier/1/is-supplier-address-in-uk")
    }

    "must render the Confirm address button posting to the AVD-S8.0 URL for the supplier" in {
      val html: String = view(ukAddress, supplierNumber)(request, msgs).toString

      html must include(msgs("supplierAddressChanged.confirmAddress"))
      html must include("""action="/nova-imports/supplier/1/add-supplier-vat"""")
    }

    "must reflect the supplier number in both onward URLs" in {
      val html: String = view(ukAddress, SupplierNumber(3))(request, msgs).toString

      html must include("/nova-imports/supplier/3/is-supplier-address-in-uk")
      html must include("/nova-imports/supplier/3/add-supplier-vat")
    }

    "must render the same content via the render method" in {
      val html: String = view.render(ukAddress, supplierNumber, request, msgs).toString

      html must include(msgs("supplierAddressChanged.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(ukAddress, supplierNumber)(request, msgs).toString

      html must include(msgs("supplierAddressChanged.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }

}
