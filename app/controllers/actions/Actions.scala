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

package controllers.actions

import com.google.inject.Inject
import models.UserAnswers
import javax.inject.Named
import models.requests.DataRequest
import play.api.mvc.{ActionBuilder, AnyContent, DefaultActionBuilder}

class Actions @Inject() (
  actionBuilder: DefaultActionBuilder,
  @Named("standard") identify: IdentifierAction,
  @Named("vatTrader") identifyVatTrader: IdentifierAction,
  @Named("vatAgent") identifyVatAgent: IdentifierAction,
  @Named("ogd") identifyOgd: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  guard: GuardAction
) {
  def authAndGetData(): ActionBuilder[DataRequest, AnyContent] =
    actionBuilder
      .andThen(identify)
      .andThen(getData)
      .andThen(requireData)

  def vatTraderAuthAndGetData(): ActionBuilder[DataRequest, AnyContent] =
    actionBuilder
      .andThen(identifyVatTrader)
      .andThen(getData)
      .andThen(requireData)

  def vatAgentAuthAndGetData(): ActionBuilder[DataRequest, AnyContent] =
    actionBuilder
      .andThen(identifyVatAgent)
      .andThen(getData)
      .andThen(requireData)

  def ogdAuthAndGetData(): ActionBuilder[DataRequest, AnyContent] =
    actionBuilder
      .andThen(identifyOgd)
      .andThen(getData)
      .andThen(requireData)

  def authAndGetDataWithGuard(predicate: UserAnswers => Boolean): ActionBuilder[DataRequest, AnyContent] =
    actionBuilder
      .andThen(identify)
      .andThen(getData)
      .andThen(requireData)
      .andThen(guard(predicate))

  def vatTraderAuthAndGetDataWithGuard(predicate: UserAnswers => Boolean): ActionBuilder[DataRequest, AnyContent] =
    actionBuilder
      .andThen(identifyVatTrader)
      .andThen(getData)
      .andThen(requireData)
      .andThen(guard(predicate))

  def vatAgentAuthAndGetDataWithGuard(predicate: UserAnswers => Boolean): ActionBuilder[DataRequest, AnyContent] =
    actionBuilder
      .andThen(identifyVatAgent)
      .andThen(getData)
      .andThen(requireData)
      .andThen(guard(predicate))

  def ogdAuthAndGetDataWithGuard(predicate: UserAnswers => Boolean): ActionBuilder[DataRequest, AnyContent] =
    actionBuilder
      .andThen(identifyOgd)
      .andThen(getData)
      .andThen(requireData)
      .andThen(guard(predicate))
}
