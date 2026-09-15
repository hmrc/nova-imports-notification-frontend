/*
 * Copyright 2024 HM Revenue & Customs
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

package views.util

import models.Country
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.SelectItem

object CountryFinderViewHelper {

  private def countryToSelectItem(countryCode: String, countryName: String): SelectItem =
    SelectItem(
      value = Some(countryCode),
      text = countryName,
      selected = false,
      attributes = Map("id" -> countryCode)
    )

  def countriesToSelectItems(euCountries: Seq[Country])(implicit messages: Messages): Seq[SelectItem] = {
    def displayName(country: Country): String =
      messages.translate(s"country.${country.code}", Nil).orElse(country.name).getOrElse(country.code)

    SelectItem() +: euCountries
      .sortBy(displayName)
      .map(country => countryToSelectItem(country.code, displayName(country)))
  }

}
