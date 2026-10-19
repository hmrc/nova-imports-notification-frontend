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

package viewmodels

import base.SpecBase
import models.{ClientList, ClientSummary}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

class ClientListCsvSpec extends SpecBase {

  private implicit val messages: Messages = stubMessages()

  private val headings = "viewClients.download.csv.name,viewClients.download.csv.reference\r\n"

  private def csvFor(clients: ClientSummary*): String = ClientListCsv(ClientList(clients, clients.size, List("C")))

  "ClientListCsv" - {

    "must write the headings followed by one row per client" in {
      csvFor(ClientSummary("Client A", "700000001"), ClientSummary("Client B", "700000002")) mustEqual
        s"${headings}Client A,700000001\r\nClient B,700000002\r\n"
    }

    "must write only the headings for an empty list" in {
      ClientListCsv(ClientList(Nil, 0, Nil)) mustEqual headings
    }

    "must quote a field that contains a comma" in {
      csvFor(ClientSummary("Smith, Jones & Co", "700000001")) mustEqual s"$headings\"Smith, Jones & Co\",700000001\r\n"
    }

    "must quote a field that contains a double quote and double it" in {
      csvFor(ClientSummary("The \"Best\" Ltd", "700000001")) mustEqual s"$headings\"The \"\"Best\"\" Ltd\",700000001\r\n"
    }

    "must quote a field that contains a line feed" in {
      csvFor(ClientSummary("Line one\nLine two", "700000001")) mustEqual s"$headings\"Line one\nLine two\",700000001\r\n"
    }

    "must quote a field that contains a carriage return" in {
      csvFor(ClientSummary("Line one\rLine two", "700000001")) mustEqual s"$headings\"Line one\rLine two\",700000001\r\n"
    }

    "must leave a non-ASCII name unquoted" in {
      csvFor(ClientSummary("Élite Systems Ltd", "760000014")) mustEqual s"${headings}Élite Systems Ltd,760000014\r\n"
    }

    "must write one row per client regardless of the total count" in {
      ClientListCsv(ClientList(List(ClientSummary("Client A", "700000001")), 42, List("A", "B"))) mustEqual
        s"${headings}Client A,700000001\r\n"
    }
  }
}
