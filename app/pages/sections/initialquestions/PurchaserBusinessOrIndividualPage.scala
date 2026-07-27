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

package pages.sections.initialquestions

import models.{PurchaserBusinessOrIndividual, UserAnswers}
import pages.QuestionPage
import pages.sections.purchaserDetails.{PurchaserBusinessNamePage, PurchaserNamePage}
import play.api.libs.json.JsPath

import scala.util.Try

case object PurchaserBusinessOrIndividualPage extends QuestionPage[PurchaserBusinessOrIndividual] {

  override def path: JsPath = JsPath \ "initial-question" \ toString

  override def toString: String = "purchaserBusinessPrivate"

  // Changing the purchaser type clears any name entered for the other type
  override def cleanup(value: Option[PurchaserBusinessOrIndividual], userAnswers: UserAnswers): Try[UserAnswers] =
    value match {
      case Some(PurchaserBusinessOrIndividual.NonVatRegisteredBusiness)          => userAnswers.remove(PurchaserNamePage)
      case Some(PurchaserBusinessOrIndividual.NonVatRegisteredPrivateIndividual) => userAnswers.remove(PurchaserBusinessNamePage)
      case _                                                                     => super.cleanup(value, userAnswers)
    }
}
