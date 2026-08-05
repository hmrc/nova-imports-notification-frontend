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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.PathBindable

class SupplierNumberSpec extends AnyFreeSpec with Matchers {

  private val binder: PathBindable[SupplierNumber] = implicitly[PathBindable[SupplierNumber]]

  "SupplierNumber.pathBindable" - {

    "must bind a URL value to the same supplier number" in {
      binder.bind("supplierNumber", "1") mustBe Right(SupplierNumber(1))
      binder.bind("supplierNumber", "2") mustBe Right(SupplierNumber(2))
    }

    "must reject zero, negative and non-numeric values" in {
      binder.bind("supplierNumber", "0").isLeft mustBe true
      binder.bind("supplierNumber", "-1").isLeft mustBe true
      binder.bind("supplierNumber", "abc").isLeft mustBe true
    }

    "must unbind a supplier number to the same URL value" in {
      binder.unbind("supplierNumber", SupplierNumber(1)) mustBe "1"
      binder.unbind("supplierNumber", SupplierNumber(3)) mustBe "3"
    }
  }
}
