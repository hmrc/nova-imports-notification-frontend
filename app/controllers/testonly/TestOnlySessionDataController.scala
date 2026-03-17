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

package controllers.testonly

import controllers.actions.IdentifierAction
import models.UserAnswers
import play.api.libs.json.*
import play.api.mvc.*
import play.twirl.api.Html
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class TestOnlySessionDataController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController {

  private val showAction   = routes.TestOnlySessionDataController.onPageLoad
  private val submitAction = routes.TestOnlySessionDataController.onSubmit

  def onPageLoad: Action[AnyContent] = identify.async { implicit request =>
    sessionRepository.get(request.userId).map {
      case Some(userAnswers) =>
        Ok(renderPage(request.userId, Json.prettyPrint(Json.toJson(userAnswers.data))))
      case None =>
        Ok(renderPage(request.userId, "{}", error = Some("No session data found. Submit to create one.")))
    }
  }

  def onSubmit: Action[AnyContent] = identify.async { implicit request =>
    request.body.asFormUrlEncoded
      .flatMap(_.get("sessionData").flatMap(_.headOption))
      .fold(Future.successful(Redirect(showAction))) { sessionData =>
        try {
          val json = Json.parse(sessionData)
          json.validate[JsObject] match {
            case JsSuccess(data, _) =>
              val userAnswers = UserAnswers(request.userId, data)
              sessionRepository.set(userAnswers).map(_ => Redirect(showAction))
            case JsError(errors) =>
              Future.successful(
                Ok(
                  renderPage(
                    request.userId,
                    sessionData,
                    Some(
                      errors
                        .map { case (path, es) =>
                          s"$path: ${es.map(_.message).mkString(", ")}"
                        }
                        .mkString("; ")
                    )
                  )
                )
              )
          }
        } catch {
          case NonFatal(e) =>
            Future.successful(Ok(renderPage(request.userId, sessionData, Some(e.getMessage))))
        }
      }
  }

  private def renderPage(userId: String, sessionData: String, error: Option[String] = None): Html = Html(
    s"""
    |<html>
    |<head>
    |<style>
    |body {font-family: monospace; padding: 1em;}
    |form {display: flex; flex-direction: column; width: 100%; height: 90vh;}
    |textarea {flex: 1; border: 1px solid #ccc; margin: 0.5em 0; padding: 0.5em; font-family: monospace; font-size: 14px;}
    |button {font-size: 1em; padding: 0.5em 1em; cursor: pointer;}
    |.error {color: red; font-weight: bold; margin: 0.5em 0;}
    |.info {color: #666; margin: 0.5em 0;}
    |</style>
    |</head>
    |<body>
    |  <h2>Session Data Editor (test-only)</h2>
    |  <div class="info">User ID: $userId</div>
    |  ${error.map(e => s"""<div class="error">$e</div>""").getOrElse("")}
    |  <form method="POST" action="${submitAction.url}">
    |    <button>Save</button>
    |    <textarea id="sessionData" name="sessionData">$sessionData</textarea>
    |    <button>Save</button>
    |  </form>
    |</body>
    |</html>
    """.stripMargin
  )
}
