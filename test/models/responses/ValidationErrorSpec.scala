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

package models.responses

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class ValidationErrorSpec extends AnyFreeSpec with Matchers {

  "ValidationError.label" - {

    "must strip the row index and map a known field key to its question text" in {
      ValidationError("make[0]", "error").label mustBe "Make"
      ValidationError("make[12]", "error").label mustBe "Make"
      ValidationError("vin[3]", "error").label mustBe "Vehicle identification number (VIN)"
      ValidationError("supplierBusinessPrivate[0]", "error").label mustBe "Is the supplier a business or private individual?"
      ValidationError("addressLine1[0]", "error").label mustBe "Address line 1"
      ValidationError("importEntryNumber[0]", "error").label mustBe "Import entry number"
      ValidationError("lcvBodyType[0]", "error").label mustBe "Light commercial vehicle body type"
      ValidationError("bodyType[0]", "error").label mustBe "Body type"
    }

    "must fall back to the field key with the index stripped when the key is not recognised" in {
      ValidationError("somethingUnexpected[0]", "error").label mustBe "somethingUnexpected"
    }

    "must fall back to the raw field key when it has no row index" in {
      ValidationError("file", "error").label mustBe "file"
    }
  }
}
