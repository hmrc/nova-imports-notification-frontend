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
import controllers.clientselection.routes
import models.{ClientSearch, ClientSearchBy, ClientSummary}
import org.scalatest.BeforeAndAfterAll
import play.api.Application
import play.api.i18n.Messages

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ClientListPageSpec extends SpecBase with BeforeAndAfterAll {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private def clients(n: Int) = (1 to n).map(i => ClientSummary(s"Client $i", s"$i"))

  private def page(clientsOnPage: Int, total: Int, current: Int, search: Option[ClientSearch] = None) =
    ClientListPage(clients(clientsOnPage), total, current, 10, search)

  private def url(n: Int, search: Option[ClientSearch] = None) =
    routes.ViewClientsController.onPageLoad(search.map(_.searchBy.jsonValue), search.map(_.search), n).url

  "ClientListPage" - {

    "must describe the records shown on the page" in {
      val p = page(10, 30, 2)

      p.from mustEqual 11
      p.to mustEqual 20
      p.totalPages mustEqual 3
    }

    "must count a partial last page" in {
      page(5, 25, 3).totalPages mustEqual 3
      page(1, 1, 1).totalPages mustEqual 1
    }

    "must show zero records from an empty page" in {
      val p = page(0, 0, 1)

      p.from mustEqual 0
      p.to mustEqual 0
      p.totalPages mustEqual 1
    }

    "must not paginate a single page" in {
      page(10, 10, 1).pagination mustBe None
    }

    "must link every page when there are only a few" in {
      val pagination = page(10, 30, 2).pagination.value

      pagination.items.value.flatMap(_.number) mustEqual Seq("1", "2", "3")
      pagination.items.value.map(_.current) mustEqual Seq(Some(false), Some(true), Some(false))
      pagination.items.value.map(_.href) mustEqual Seq(url(1), url(2), url(3))
      pagination.previous.value.href mustEqual url(1)
      pagination.next.value.href mustEqual url(3)
    }

    "must omit previous on the first page and next on the last" in {
      page(10, 30, 1).pagination.value.previous mustBe None
      page(10, 30, 3).pagination.value.next mustBe None
    }

    "must collapse distant pages into ellipses" in {
      val items = page(10, 100, 5).pagination.value.items.value

      items.map(i => i.number.getOrElse("…")) mustEqual Seq("1", "…", "4", "5", "6", "…", "10")
      items.filter(_.ellipsis.contains(true)).size mustEqual 2
    }

    "must keep the neighbours of the first and last pages" in {
      page(10, 100, 1).pagination.value.items.value.map(_.number.getOrElse("…")) mustEqual Seq("1", "2", "…", "10")
      page(10, 100, 10).pagination.value.items.value.map(_.number.getOrElse("…")) mustEqual Seq("1", "…", "9", "10")
    }

    "must keep the search in page links" in {
      val search     = Some(ClientSearch(ClientSearchBy.Name, "Client"))
      val pagination = page(10, 30, 1, search).pagination.value

      pagination.next.value.href mustEqual url(2, search)
      pagination.items.value.head.href mustEqual url(1, search)
    }
  }
}
