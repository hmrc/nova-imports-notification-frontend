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

package forms.mappings

import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.i18n.Messages

import java.time.{LocalDate, Month}
import scala.util.Try

private[mappings] class LocalDateFormatter(
  invalidKey: String,
  allRequiredKey: String,
  twoRequiredKey: String,
  requiredKey: String,
  notARealDateKey: String,
  requireDdMmYyyy: Boolean = false,
  args: Seq[String] = Seq.empty
)(implicit messages: Messages)
    extends Formatter[LocalDate] {

  private val fieldKeys: List[String] = List("day", "month", "year")

  private val dayRange          = 1 to 31
  private val monthRange        = 1 to 12
  private val dayAndMonthLength = 2
  private val yearLength        = 4

  private val number = "^\\d+$".r

  private def isNumber(value: String): Boolean = number.matches(value)

  // Where the date is not held as dd/MM/yyyy the input also accepts "March" or "Mar" in place of "3"
  private def monthValue(month: String): Option[Int] =
    if (isNumber(month)) month.toIntOption
    else
      Month.values.toList
        .find(m => m.toString == month.toUpperCase || m.toString.take(3) == month.toUpperCase)
        .map(_.getValue)

  // dd/MM/yyyy needs every part to be a number padded to a fixed width, so "7" is not a day and "Mar" is not a month
  private def hasValidFormat(field: String, value: String): Boolean = field match {
    case "year"               => isNumber(value) && value.length == yearLength
    case _ if requireDdMmYyyy => isNumber(value) && value.length == dayAndMonthLength
    case "day"                => isNumber(value)
    case _                    => monthValue(value).isDefined
  }

  // A part the service cannot read is a format problem. Parts that are readable numbers but do not make up a date
  // that exists are reported separately.
  private def formatDate(key: String, parts: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val unreadableFields = fieldKeys.filterNot(field => hasValidFormat(field, parts(field)))

    if (unreadableFields.nonEmpty) {
      Left(Seq(error(key, invalidKey, unreadableFields)))
    } else {

      val dayInRange   = parts("day").toIntOption.filter(dayRange.contains)
      val monthInRange = monthValue(parts("month")).filter(monthRange.contains)

      val outOfRangeFields = fieldKeys.filter {
        case "day"   => dayInRange.isEmpty
        case "month" => monthInRange.isEmpty
        case _       => false
      }

      if (outOfRangeFields.nonEmpty) {
        Left(Seq(error(key, notARealDateKey, outOfRangeFields)))
      } else {
        val date = for {
          dayOfMonth  <- dayInRange
          monthOfYear <- monthInRange
          yearNumber  <- parts("year").toIntOption
          date        <- Try(LocalDate.of(yearNumber, monthOfYear, dayOfMonth)).toOption
        } yield date

        date.toRight(Seq(error(key, notARealDateKey, Nil)))
      }
    }
  }

  // Naming a single field highlights just that field. When more than one is wrong it is not clear what the user
  // meant, so no field is named and the date input is highlighted as a whole.
  private def error(key: String, errorKey: String, fields: Seq[String]): FormError = {
    val fieldArgs = if (fields.sizeIs == 1) fields.map(field => messages(s"date.error.$field")) else Seq.empty
    FormError(key, errorKey, fieldArgs ++ args)
  }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val fields = fieldKeys.map { field =>
      field -> data.get(s"$key.$field").map(_.trim).filter(_.nonEmpty)
    }.toMap

    lazy val missingFields = fieldKeys.filter(fields(_).isEmpty).map(field => messages(s"date.error.$field"))

    if (fields.values.forall(_.isDefined)) {
      formatDate(key, fields.collect { case (field, Some(value)) => field -> value })
    } else {
      missingFields match {
        case _ :: Nil      => Left(List(FormError(key, requiredKey, missingFields ++ args)))
        case _ :: _ :: Nil => Left(List(FormError(key, twoRequiredKey, missingFields ++ args)))
        case _             => Left(List(FormError(key, allRequiredKey, args)))
      }
    }
  }

  // A dd/MM/yyyy date is put back into the fields padded, so that a date the user comes back to still binds
  override def unbind(key: String, value: LocalDate): Map[String, String] = {
    val dayOrMonth: Int => String = if (requireDdMmYyyy) part => f"$part%02d" else _.toString

    Map(
      s"$key.day"   -> dayOrMonth(value.getDayOfMonth),
      s"$key.month" -> dayOrMonth(value.getMonthValue),
      s"$key.year"  -> value.getYear.toString
    )
  }
}
