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

import com.google.inject.Inject
import config.FrontendAppConfig
import models.{DraftId, NotificationSummary}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

sealed trait CreateDraftError
object CreateDraftError {
  case object ClientNotFound extends CreateDraftError
  final case class UpstreamError(status: Int, message: String) extends CreateDraftError
}

sealed trait GetNotificationSummaryError
object GetNotificationSummaryError {
  final case class UpstreamError(status: Int, message: String) extends GetNotificationSummaryError
}

sealed trait UpdateSectionError
object UpdateSectionError {
  case object Forbidden extends UpdateSectionError
  case object NotFound extends UpdateSectionError
  final case class UpstreamError(status: Int, message: String) extends UpdateSectionError
}

trait NovaImportsBackendConnector {

  def createDraft(clientVrn: Option[String])(implicit hc: HeaderCarrier): Future[Either[CreateDraftError, DraftId]]

  def getNotificationSummary(clientVrn: Option[String])(implicit hc: HeaderCarrier): Future[Either[GetNotificationSummaryError, NotificationSummary]]

  def updateDraftSection(draftId: DraftId, sectionId: String, body: JsObject)(implicit hc: HeaderCarrier): Future[Either[UpdateSectionError, Unit]]
}

class NovaImportsBackendConnectorImpl @Inject() (
  httpClient: HttpClientV2,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends NovaImportsBackendConnector {

  private def serviceUrl(path: String): String =
    s"${appConfig.novaImportsBackendBaseUrl}/nova-imports$path"

  override def createDraft(clientVrn: Option[String])(implicit hc: HeaderCarrier): Future[Either[CreateDraftError, DraftId]] = {
    import CreateDraftError.*

    val request  = httpClient.post(url"${serviceUrl("/draft-notifications")}")
    val withBody = clientVrn match {
      case Some(vrn) => request.withBody(Json.obj("clientVrn" -> vrn))
      case None      => request
    }

    withBody.execute[HttpResponse].map { response =>
      response.status match {
        case 201 =>
          (response.json \ "draftId")
            .asOpt[String]
            .map(id => Right(DraftId(id)))
            .getOrElse(Left(UpstreamError(201, "Missing draftId in response body")))
        case 403 => Left(ClientNotFound)
        case s   => Left(UpstreamError(s, response.body))
      }
    }
  }

  override def getNotificationSummary(
    clientVrn: Option[String]
  )(implicit hc: HeaderCarrier): Future[Either[GetNotificationSummaryError, NotificationSummary]] = {
    import GetNotificationSummaryError.*

    val request  = httpClient.get(url"${serviceUrl("/notification-summary")}")
    val withBody = clientVrn match {
      case Some(vrn) => request.withBody(Json.obj("clientVrn" -> vrn))
      case None      => request
    }

    withBody
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case 200 =>
            response.json
              .validate[NotificationSummary]
              .map(Right(_))
              .recoverTotal(err => Left(UpstreamError(200, s"Malformed notification summary: $err")))
          case s => Left(UpstreamError(s, response.body))
        }
      }
  }

  override def updateDraftSection(draftId: DraftId, sectionId: String, body: JsObject)(implicit
    hc: HeaderCarrier
  ): Future[Either[UpdateSectionError, Unit]] = {
    import UpdateSectionError.*

    httpClient
      .put(url"${serviceUrl(s"/draft-notifications/${draftId.value}/sections/$sectionId")}")
      .withBody(body)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case 200 => Right(())
          case 403 => Left(Forbidden)
          case 404 => Left(NotFound)
          case s   => Left(UpstreamError(s, response.body))
        }
      }
  }
}
