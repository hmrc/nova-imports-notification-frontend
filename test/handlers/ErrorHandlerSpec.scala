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

package handlers

import base.SpecBase
import config.FrontendAppConfig
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.http.Status.NOT_FOUND
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ErrorHandlerSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages      = messages(app)
  val handler: ErrorHandler        = app.injector.instanceOf[ErrorHandler]
  val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "ErrorHandler.notFoundTemplate" - {
    "must render the Page not found content" in {
      val html: String = Await.result(handler.notFoundTemplate(FakeRequest()), 10.seconds).toString

      html must include(msgs("pageNotFound.heading"))
      html must include(msgs("pageNotFound.paragraph.1"))
      html must include(msgs("pageNotFound.link.text"))
      html must include(appConfig.technicalSupportUrl)
    }
  }

  "ErrorHandler.onClientError" - {
    "must return 404 with the Page not found content for an unknown page" in {
      val result = handler.onClientError(FakeRequest(), NOT_FOUND, "")

      status(result) mustBe NOT_FOUND
      contentAsString(result) must include(msgs("pageNotFound.heading"))
    }
  }

}
