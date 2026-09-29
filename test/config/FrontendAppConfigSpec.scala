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

package config

import base.SpecBase
import models.{AddressJourney, Country, Currency, SupplierNumber}
import org.scalatest.BeforeAndAfterAll
import play.api.Application

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class FrontendAppConfigSpec extends SpecBase with BeforeAndAfterAll {

  private val app: Application = applicationBuilder(userAnswers = None).build()
  private val appConfig        = app.injector.instanceOf[FrontendAppConfig]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "addressLookupCallbackUrl" - {

    "must be the notifier callback, prefixed exactly once" in {
      appConfig.addressLookupCallbackUrl(AddressJourney.Notifier) mustEqual
        s"${appConfig.host}/nova-imports/add-address/address-lookup-callback"
    }

    "must be the supplier callback for the supplier number given" in {
      appConfig.addressLookupCallbackUrl(AddressJourney.Supplier(SupplierNumber(2))) mustEqual
        s"${appConfig.host}/nova-imports/supplier/2/address-lookup-callback"
    }
  }

  "countries" - {

    "must load every country from the bundled list" in {
      appConfig.countries.size mustEqual 249
    }

    "must load each entry as a code and a name" in {
      appConfig.countries must contain(Country("FR", "France"))
      appConfig.countries must contain(Country("CZ", "Czech Republic"))
    }

    "must offer the United Kingdom" in {
      appConfig.countries must contain(Country("GB", "United Kingdom"))
    }

    "must not offer codes missing from the official list" in {
      appConfig.countries.map(_.code) must contain noneOf ("XK", "SS")
    }

    "must keep both names listed against the Vatican code" in {
      appConfig.countries.filter(_.code == "VA") mustEqual List(
        Country("VA", "Holy See (Vatican City State)"),
        Country("VA", "Vatican City State")
      )
    }
  }

  "currencies" - {

    "must load every currency from the bundled list" in {
      appConfig.currencies.size mustEqual 158
    }

    "must load each entry as a code and a name" in {
      appConfig.currencies must contain(Currency("AFN", "Afghani"))
      appConfig.currencies must contain(Currency("ZWD", "Zimbabwean dollar"))
    }

    "must have no duplicate codes" in {
      appConfig.currencies.map(_.code).distinct.size mustEqual appConfig.currencies.size
    }
  }

  "supplierCurrencies" - {

    "must offer only the currencies agreed for the supplier journey, in the bundled order" in {
      appConfig.supplierCurrencies.map(_.code) mustEqual
        List("GBP", "BGN", "CZK", "DKK", "EUR", "HUF", "LTL", "PLN", "RON", "SEK", "USD")
    }
  }
}
