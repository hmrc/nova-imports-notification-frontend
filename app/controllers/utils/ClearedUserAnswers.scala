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

package controllers.utils

import models.{UserAnswers, UserContext}
import pages.AgentSelectedClientPage

import scala.util.{Success, Try}

object ClearedUserAnswers {

  // keeps the selected client for an agent with a client
  def apply(userId: String, answers: UserAnswers, ctx: UserContext): Try[UserAnswers] =
    (if (ctx.isAgentWithClient) answers.get(AgentSelectedClientPage) else None) match {
      case Some(client) => UserAnswers(userId).set(AgentSelectedClientPage, client)
      case None         => Success(UserAnswers(userId))
    }
}
