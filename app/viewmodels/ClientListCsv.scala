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

import models.ClientList
import play.api.i18n.Messages

object ClientListCsv {

  def apply(clientList: ClientList)(implicit messages: Messages): String = {
    val headings = record(messages("viewClients.download.csv.name"), messages("viewClients.download.csv.reference"))
    val rows     = clientList.clients.map(client => record(client.name, client.vatRegistrationNumber))
    (headings +: rows).mkString
  }

  private def record(name: String, reference: String): String =
    s"${field(name)},${field(reference)}\r\n"

  private def field(value: String): String =
    if (value.exists(c => c == ',' || c == '"' || c == '\r' || c == '\n')) s""""${value.replace("\"", "\"\"")}""""
    else value
}
