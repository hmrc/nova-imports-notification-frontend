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

package views

import base.SpecBase
import forms.DateOfFirstRegistrationKnownFormProvider
import models.{ImportNumber, NormalMode, VehicleNumber}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.DateOfFirstRegistrationKnownView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*

class DateOfFirstRegistrationKnownViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: DateOfFirstRegistrationKnownView = app.injector.instanceOf[DateOfFirstRegistrationKnownView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form = new DateOfFirstRegistrationKnownFormProvider()()

  private val submitCall =
    controllers.vehicledetails.routes.DateOfFirstRegistrationKnownController.onSubmit(ImportNumber(1), VehicleNumber(1), NormalMode)

  private def doc(f: Form[Boolean] = form): Document = Jsoup.parse(view(f, submitCall)(request, msgs).toString)

  "DateOfFirstRegistrationKnownView" - {

    "must render the page title" in {
      doc().title must startWith(msgs("dateOfFirstRegistrationKnown.title"))
    }

    "must render the heading as the legend of the radios" in {
      val heading = doc().select("legend h1")
      heading.text mustEqual msgs("dateOfFirstRegistrationKnown.heading")
    }

    "must render the caption" in {
      doc().select("span.govuk-caption-l").text mustEqual msgs("dateOfFirstRegistrationKnown.caption")
    }

    "must render Yes and No radio options" in {
      doc().select(".govuk-radios__label").asScala.map(_.text).toSeq mustEqual Seq(msgs("site.yes"), msgs("site.no"))
    }

    "must render the Continue button" in {
      doc().select("button.govuk-button").text mustEqual msgs("site.continue")
    }

    "must post to the submit call" in {
      doc().select("form").attr("action") mustEqual submitCall.url
    }

    "must render the error summary and field error when nothing is selected" in {
      val errorDoc = doc(form.bind(Map("value" -> "")))

      errorDoc.select(".govuk-error-summary").text must include(msgs("dateOfFirstRegistrationKnown.error.required"))
      errorDoc.select(".govuk-error-message").text must include(msgs("dateOfFirstRegistrationKnown.error.required"))
      errorDoc.title must startWith(msgs("error.title.prefix"))
    }
  }
}
