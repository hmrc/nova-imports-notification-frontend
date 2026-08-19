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

package pages.sections.supplierdetails

import models.{BusinessOrPrivateIndividual, UserAnswers}
import pages.QuestionPage
import play.api.libs.json.JsPath

import scala.util.Try

case object SupplierBusinessOrIndividualPage extends QuestionPage[BusinessOrPrivateIndividual] {

  override def path: JsPath = JsPath \ "supplier-details" \ toString

  override def toString: String = "supplierBusinessOrIndividual"

  // Changing the supplier type clears any name entered for the other type.
  override def cleanup(value: Option[BusinessOrPrivateIndividual], userAnswers: UserAnswers): Try[UserAnswers] =
    value match {
      case Some(BusinessOrPrivateIndividual.Business)          => userAnswers.remove(SupplierNamePage)
      case Some(BusinessOrPrivateIndividual.PrivateIndividual) => userAnswers.remove(SupplierBusinessNamePage)
      case _                                                   => super.cleanup(value, userAnswers)
    }
}
