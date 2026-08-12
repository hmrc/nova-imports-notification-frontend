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
import models.{AddressJourney, SupplierNumber}
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
}
