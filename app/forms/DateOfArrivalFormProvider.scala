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

import forms.DateOfArrivalFormProvider.FirstRegistration
import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms.optional
import play.api.data.validation.Constraint
import play.api.i18n.Messages

import java.time.{Clock, LocalDate, ZoneId}
import javax.inject.Inject

class DateOfArrivalFormProvider @Inject() (clock: Clock) extends Mappings {

  def apply(isRequired: Boolean, firstRegistration: Option[FirstRegistration] = None)(implicit messages: Messages): Form[Option[LocalDate]] =
    Form(
      "value" -> optional(
        localDate(
          invalidKey = "dateOfArrival.error.invalid",
          allRequiredKey = "dateOfArrival.error.required.all",
          twoRequiredKey = "dateOfArrival.error.required.two",
          requiredKey = "dateOfArrival.error.required",
          notARealDateKey = "dateOfArrival.error.notARealDate",
          requireDdMmYyyy = true
        ).verifying(firstError(maxDate(todayInUk, "dateOfArrival.error.future") +: firstRegistration.map(afterFirstRegistration).toList*))
      ).verifying("dateOfArrival.error.required.all", date => !isRequired || date.isDefined)
    )

  private def todayInUk: LocalDate = LocalDate.now(clock.withZone(ZoneId.of("Europe/London")))

  private def afterFirstRegistration(firstRegistration: FirstRegistration): Constraint[LocalDate] =
    if (firstRegistration.countryIsEuOrUk) minDate(firstRegistration.date.plusDays(1), "dateOfArrival.error.afterFirstRegistration")
    else minDate(firstRegistration.date, "dateOfArrival.error.sameAsOrAfterFirstRegistration")
}

object DateOfArrivalFormProvider {

  final case class FirstRegistration(date: LocalDate, countryIsEuOrUk: Boolean)
}
