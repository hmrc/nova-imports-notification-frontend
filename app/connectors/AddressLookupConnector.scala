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
import models.Address
import play.api.libs.json.{JsError, JsObject, JsSuccess}
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

sealed trait AddressLookupError
object AddressLookupError {
  case object MissingJourneyUrl extends AddressLookupError
  case object NotFound extends AddressLookupError
  final case class UpstreamError(status: Int, message: String) extends AddressLookupError
}

trait AddressLookupConnector {

  def initJourney(config: JsObject)(implicit hc: HeaderCarrier): Future[Either[AddressLookupError, String]]

  def confirmedAddress(journeyId: String)(implicit hc: HeaderCarrier): Future[Either[AddressLookupError, Address]]
}

class AddressLookupConnectorImpl @Inject() (
  httpClient: HttpClientV2,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends AddressLookupConnector {

  import AddressLookupError.*

  private def serviceUrl(path: String): String =
    s"${appConfig.addressLookupFrontendBaseUrl}$path"

  override def initJourney(config: JsObject)(implicit hc: HeaderCarrier): Future[Either[AddressLookupError, String]] =
    httpClient
      .post(url"${serviceUrl("/api/v2/init")}")
      .withBody(config)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case 202 =>
            response.header("Location").toRight(MissingJourneyUrl)
          case s => Left(UpstreamError(s, response.body))
        }
      }

  override def confirmedAddress(journeyId: String)(implicit hc: HeaderCarrier): Future[Either[AddressLookupError, Address]] =
    httpClient
      .get(url"${serviceUrl(s"/api/v2/confirmed?id=$journeyId")}")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case 200 =>
            (response.json \ "address").validate[Address] match {
              case JsSuccess(address, _) => Right(address)
              case JsError(errors)       => Left(UpstreamError(200, s"Malformed address response: $errors"))
            }
          case 404 => Left(NotFound)
          case s   => Left(UpstreamError(s, response.body))
        }
      }
}
