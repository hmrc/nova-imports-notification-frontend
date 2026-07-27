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

import models.{PurchaserOrOnBehalf, UserAnswers}
import pages.QuestionPage
import pages.sections.purchaserDetails.{PurchaserBusinessNamePage, PurchaserNamePage}
import play.api.libs.json.JsPath

import scala.util.Try

case object PurchaserOrOnBehalfPage extends QuestionPage[PurchaserOrOnBehalf] {

  override def path: JsPath = JsPath \ "initial-question" \ toString

  override def toString: String = "purchaserOrOnBehalf"

  // User is the purchaser, so there is no separate purchaser to capture. Clear the type and name answers
  override def cleanup(value: Option[PurchaserOrOnBehalf], userAnswers: UserAnswers): Try[UserAnswers] =
    value match {
      case Some(PurchaserOrOnBehalf.Purchaser) =>
        for {
          a1 <- userAnswers.remove(PurchaserBusinessOrIndividualPage)
          a2 <- a1.remove(PurchaserNamePage)
          a3 <- a2.remove(PurchaserBusinessNamePage)
        } yield a3
      case _ => super.cleanup(value, userAnswers)
    }
}
