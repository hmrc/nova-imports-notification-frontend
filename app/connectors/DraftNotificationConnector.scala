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
import models.DraftId
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

sealed trait CreateDraftError
object CreateDraftError {
  case object ClientNotFound                                   extends CreateDraftError
  final case class UpstreamError(status: Int, message: String) extends CreateDraftError
}

trait DraftNotificationConnector {
  def createDraft(clientVrn: Option[String])(implicit hc: HeaderCarrier): Future[Either[CreateDraftError, DraftId]]
}

class DraftNotificationConnectorStub @Inject() (implicit ec: ExecutionContext) extends DraftNotificationConnector {

  override def createDraft(clientVrn: Option[String])(implicit hc: HeaderCarrier): Future[Either[CreateDraftError, DraftId]] =
    Future.successful(Right(DraftId("STUB-12345")))
}

class DraftNotificationConnectorImpl @Inject() (
  httpClient: HttpClientV2,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends DraftNotificationConnector {

  import CreateDraftError.*

  override def createDraft(clientVrn: Option[String])(implicit hc: HeaderCarrier): Future[Either[CreateDraftError, DraftId]] = {
    val endpoint = url"${appConfig.novaImportsBackendBaseUrl}/draft-notifications"
    val request  = httpClient.post(endpoint)
    val withBody = clientVrn match {
      case Some(vrn) => request.withBody(Json.obj("clientVrn" -> vrn): JsObject)
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
}
