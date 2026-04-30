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

package connectors

import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class DraftNotificationConnectorStubSpec extends AnyFreeSpec with Matchers with ScalaFutures with OptionValues {

  private given hc: HeaderCarrier = HeaderCarrier()

  private val connector = new DraftNotificationConnectorStub()

  "DraftNotificationConnectorStub" - {

    "returns a stub draft id for an individual / org / agent-without-client" in {
      connector.createDraft(clientVrn = None).futureValue.toOption.value.value must startWith("STUB-")
    }

    "returns a stub draft id for an agent-with-client" in {
      connector.createDraft(clientVrn = Some("GB123456789")).futureValue.toOption.value.value must startWith("STUB-")
    }
  }
}
