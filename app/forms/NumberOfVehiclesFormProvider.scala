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
import models.{NumberOfVehicles, OneOrMultipleVehicles}
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}
import play.api.data.Forms.{mapping, of}

import javax.inject.Inject

class NumberOfVehiclesFormProvider @Inject() extends Mappings {

  import NumberOfVehiclesFormProvider.*

  def apply(): Form[NumberOfVehicles] = Form(
    mapping(
      OneOrMultipleVehiclesField -> enumerable[OneOrMultipleVehicles]("numberOfVehicles.error.required"),
      NumberOfVehiclesField      -> of(numberOfVehicles)
    )(NumberOfVehicles.apply)(answer => Some((answer.oneOrMultipleVehicles, answer.numberOfVehiclesToMultiply)))
  )

  private val numberOfVehicles: Formatter[Option[Int]] = new Formatter[Option[Int]] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[Int]] =
      if (data.get(OneOrMultipleVehiclesField).contains(OneOrMultipleVehicles.Multiple.jsonValue))
        data.get(key).filter(_.trim.nonEmpty) match {
          case Some(number) if number.matches(NumberOfVehiclesRegex) => Right(Some(number.toInt))
          case Some(_)                                               => Left(Seq(FormError(key, "numberOfVehicles.error.input.format")))
          case None                                                  => Left(Seq(FormError(key, "numberOfVehicles.error.input.required")))
        }
      else Right(None)

    override def unbind(key: String, value: Option[Int]): Map[String, String] =
      value.map(number => key -> number.toString).toMap
  }
}

object NumberOfVehiclesFormProvider {
  val OneOrMultipleVehiclesField: String = "oneOrMultipleVehicles"
  val NumberOfVehiclesField: String      = "numberOfVehiclesToMultiply"
  val NumberOfVehiclesRegex: String      = """^([2-9]|[1-9][0-9]|100)$"""
}
