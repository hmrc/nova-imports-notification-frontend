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

import base.SpecBase
import models.{NumberOfVehicles, OneMultiVehicle}
import play.api.data.FormError

class NumberOfVehiclesFormProviderSpec extends SpecBase {

  val form = new NumberOfVehiclesFormProvider()()

  "NumberOfVehiclesFormProvider" - {

    "bind 'one' with no numberSameVehicles, ignoring numberSameVehicles entirely even if supplied" in {
      form.bind(Map("oneMultiVehicle" -> "one", "numberSameVehicles" -> "999")).value.value mustEqual
        NumberOfVehicles(OneMultiVehicle.One, None)
    }

    "bind 'more' with a valid numberSameVehicles" in {
      form.bind(Map("oneMultiVehicle" -> "more", "numberSameVehicles" -> "5")).value.value mustEqual
        NumberOfVehicles(OneMultiVehicle.More, Some(5))
    }

    "accept the boundary value 2" in {
      form.bind(Map("oneMultiVehicle" -> "more", "numberSameVehicles" -> "2")).value.value.numberSameVehicles mustEqual Some(2)
    }

    "accept the boundary value 100" in {
      form.bind(Map("oneMultiVehicle" -> "more", "numberSameVehicles" -> "100")).value.value.numberSameVehicles mustEqual Some(100)
    }

    "reject a missing selection" in {
      form.bind(Map("numberSameVehicles" -> "5")).errors must contain(FormError("oneMultiVehicle", "numberOfVehicles.error.required.selection"))
    }

    "reject 'more' with a blank numberSameVehicles" in {
      form.bind(Map("oneMultiVehicle" -> "more", "numberSameVehicles" -> "")).errors must contain(
        FormError("numberSameVehicles", "numberOfVehicles.error.required.count")
      )
    }

    "reject 'more' with numberSameVehicles below the range (1)" in {
      form.bind(Map("oneMultiVehicle" -> "more", "numberSameVehicles" -> "1")).errors must contain(
        FormError("numberSameVehicles", "numberOfVehicles.error.length")
      )
    }

    "reject 'more' with numberSameVehicles above the range (101)" in {
      form.bind(Map("oneMultiVehicle" -> "more", "numberSameVehicles" -> "101")).errors must contain(
        FormError("numberSameVehicles", "numberOfVehicles.error.length")
      )
    }

    "reject 'more' with a non-numeric numberSameVehicles" in {
      form.bind(Map("oneMultiVehicle" -> "more", "numberSameVehicles" -> "abc")).errors must contain(
        FormError("numberSameVehicles", "numberOfVehicles.error.length")
      )
    }
  }
}
