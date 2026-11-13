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

package models

enum VatReliefReason(val jsonValue: String) {
  case ModifiedForDisabledPassenger extends VatReliefReason("MODIFIED_FOR_DISABLED_PASSENGER")
  case SubjectToReliefForCharity extends VatReliefReason("SUBJECT_TO_RELIEF_FOR_CHARITY")
  case ReturnedGoodsRelief extends VatReliefReason("RETURNED_GOODS_RELIEF")
  case InheritedGoods extends VatReliefReason("INHERITED_GOODS")
  case TransferOfResidence extends VatReliefReason("TRANSFER_OF_RESIDENCE")

  override def toString: String = jsonValue
}

object VatReliefReason extends Enumerable.Implicits {

  // AVD11.1a - supplier journey: only these two reasons are offered
  val supplierOptions: Seq[VatReliefReason] = Seq(ModifiedForDisabledPassenger, SubjectToReliefForCharity)

  // AVD11.1b - import journey: all five reasons are offered, in this order
  val importOptions: Seq[VatReliefReason] = Seq(
    InheritedGoods,
    ReturnedGoodsRelief,
    TransferOfResidence,
    ModifiedForDisabledPassenger,
    SubjectToReliefForCharity
  )

  given Enumerable[VatReliefReason] = enumerable(importOptions)

  def enumerable(options: Seq[VatReliefReason]): Enumerable[VatReliefReason] =
    Enumerable(options.map(option => option.jsonValue -> option)*)
}
