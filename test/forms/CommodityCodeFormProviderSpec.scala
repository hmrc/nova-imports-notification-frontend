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

package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class CommodityCodeFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "commodityCode.error.required"
  val lengthKey   = "commodityCode.error.length"
  val formatKey   = "commodityCode.error.format"

  val form = new CommodityCodeFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must bind numeric commodity codes between 4 and 10 digits" in {

      val validCodes = List("2134", "87031", "870312", "8703123", "87031234", "870312345", "8703123456")

      validCodes.foreach { code =>
        val result = form.bind(Map(fieldName -> code))
        result.errors mustBe empty
        result.value mustBe Some(code)
      }
    }

    "must not bind commodity codes shorter than 4 or longer than 10 characters" in {

      val wrongLengths = List("1", "123", "87031234567", "870312345678", "ABC", "8703 12 34 56")

      wrongLengths.foreach { code =>
        val result = form.bind(Map(fieldName -> code)).apply(fieldName)
        result.errors must contain only FormError(fieldName, lengthKey)
      }
    }

    "must not bind commodity codes of the right length that contain characters other than digits" in {

      val invalidCodes = List("ABCD", "8703A23456", "8703.12", "8703 123", "-2134", "87031234 ")

      invalidCodes.foreach { code =>
        val result = form.bind(Map(fieldName -> code)).apply(fieldName)
        result.errors must contain only FormError(fieldName, formatKey, Seq(CommodityCodeFormProvider.CommodityCodeRegex))
      }
    }

    "must not bind an entry of only whitespace" in {

      val result = form.bind(Map(fieldName -> "   "))

      result.errors must contain only FormError(fieldName, requiredKey)
    }
  }
}
