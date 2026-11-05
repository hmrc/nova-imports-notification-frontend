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

import forms.mappings.Mappings
import models.{NumberOfVehicles, OneMultiVehicle}
import play.api.data.Forms.{mapping, of}
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}

import javax.inject.Inject

class NumberOfVehiclesFormProvider @Inject() extends Mappings {

  private val oneMultiVehicleKey    = "oneMultiVehicle"
  private val numberSameVehiclesKey = "numberSameVehicles"
  private val numberRegex           = "^([2-9]|[1-9][0-9]|100)$".r

  private def numberSameVehiclesFormatter: Formatter[Option[Int]] = new Formatter[Option[Int]] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[Int]] = {
      val isMultiple = data.get(oneMultiVehicleKey).contains(OneMultiVehicle.More.jsonValue)
      val raw        = data.getOrElse(key, "").trim

      if (!isMultiple) Right(None)
      else if (raw.isEmpty) Left(Seq(FormError(key, "numberOfVehicles.error.required.count")))
      else
        numberRegex.findFirstIn(raw) match {
          case Some(_) => Right(Some(raw.toInt))
          case None    => Left(Seq(FormError(key, "numberOfVehicles.error.length")))
        }
    }

    override def unbind(key: String, value: Option[Int]): Map[String, String] =
      value.map(v => key -> v.toString).toMap
  }

  def apply(): Form[NumberOfVehicles] =
    Form(
      mapping(
        oneMultiVehicleKey    -> enumerable[OneMultiVehicle]("numberOfVehicles.error.required.selection"),
        numberSameVehiclesKey -> of(numberSameVehiclesFormatter)
      )(NumberOfVehicles.apply)(numberOfVehicles => Some((numberOfVehicles.oneMultiVehicle, numberOfVehicles.numberSameVehicles)))
    )
}
