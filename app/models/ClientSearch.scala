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

enum ClientSearchBy(val jsonValue: String) {
  case Name extends ClientSearchBy("name")
  case Vrn extends ClientSearchBy("vrn")

  override def toString: String = jsonValue
}

object ClientSearchBy extends Enumerable.Implicits {
  given Enumerable[ClientSearchBy] = Enumerable(
    Name.jsonValue -> Name,
    Vrn.jsonValue  -> Vrn
  )
}

final case class ClientSearch(searchBy: ClientSearchBy, search: String)

object ClientSearch {

  def from(searchBy: Option[String], search: Option[String]): Option[ClientSearch] =
    for {
      by   <- searchBy.flatMap(summon[Enumerable[ClientSearchBy]].withName)
      term <- search.map(_.trim).filter(_.nonEmpty)
    } yield ClientSearch(by, term)
}

final case class ClientListQuery(vrn: Option[String] = None, name: Option[String] = None, start: Int = 0, count: Int = -1)

object ClientListQuery {

  def apply(search: Option[ClientSearch], start: Int, count: Int): ClientListQuery =
    search match {
      case Some(ClientSearch(ClientSearchBy.Vrn, term))  => ClientListQuery(vrn = Some(term), start = start, count = count)
      case Some(ClientSearch(ClientSearchBy.Name, term)) => ClientListQuery(name = Some(term), start = start, count = count)
      case None                                          => ClientListQuery(start = start, count = count)
    }
}
