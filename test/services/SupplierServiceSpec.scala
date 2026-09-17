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

package services

import base.SpecBase
import connectors.{GetTraderInformationError, NovaImportsBackendConnector}
import models.{BusinessOrPrivateIndividual, NameDetails, NovaUserType, SupplierNumber, TraderInformation, UserAnswers, UserContext}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.BusinessOrPrivatePage
import pages.sections.notifierdetails.{BusinessNamePage, NameDetailsPage}
import pages.sections.purchaserdetails.{PurchaserBusinessNamePage, PurchaserNamePage}
import pages.sections.supplierdetails.{SupplierBusinessNamePage, SupplierBusinessOrIndividualPage, SupplierNamePage, UsePersonalDetailsAsSupplierPage, UsePurchaserDetailsAsSupplierPage}
import play.api.libs.json.{JsObject, Json}
import queries.AllSuppliersQuery
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

class SupplierServiceSpec extends SpecBase with MockitoSugar {

  private def newService(
    repo: SessionRepository,
    vehicles: VehicleService = mock[VehicleService],
    connector: NovaImportsBackendConnector = mock[NovaImportsBackendConnector]
  ): SupplierService =
    new SupplierServiceImpl(repo, vehicles, connector)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val supplierNumber = SupplierNumber(1)

  private def userContextFor(userType: NovaUserType, isForBusinessUse: Boolean = false): UserContext =
    UserContext(
      userType = userType,
      selectedClient = None,
      notDeregistered = true,
      isAgentWithClientNoEnrolments = false,
      agentHasVatAgentEnrolment = false,
      isForBusinessUse = isForBusinessUse
    )

  private val traderInformation: TraderInformation = TraderInformation(
    traderName = Some("Test Co"),
    tradingName = Some("Test Trading"),
    addressLine1 = Some("1 High Street"),
    addressLine2 = Some("Testtown"),
    addressLine3 = None,
    addressLine4 = None,
    postcode = Some("ZZ01 1ZZ")
  )

  private def connectorReturning(result: Either[GetTraderInformationError, TraderInformation]): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getTraderInformation()(any())).thenReturn(Future.successful(result))
    connector
  }

  private def mockSessionRepoThatSaves(answers: UserAnswers): SessionRepository = {
    val repo = mock[SessionRepository]
    when(repo.setPage(any(), eqTo(AllSuppliersQuery), any())(any())).thenReturn(Future.successful(answers))
    when(repo.set(any())).thenReturn(Future.successful(true))
    repo
  }

  private def mockVehicleServiceThatRemoves(answers: UserAnswers): VehicleService = {
    val vehicles = mock[VehicleService]
    when(vehicles.deleteValuesForSupplier(any(), any())).thenReturn(Success(answers))
    vehicles
  }

  private val deleted: JsObject = Json.obj("deleted" -> true)

  private val answered: JsObject = Json.obj("supplierBusinessName" -> "Test Co")

  "SupplierService.add" - {

    "must return supplier number 1 when there are no suppliers yet" in {
      val repo = mockSessionRepoThatSaves(emptyUserAnswers)

      val number = newService(repo).add(emptyUserAnswers).futureValue

      number mustBe SupplierNumber(1)
    }

    "must store the new supplier as an empty record" in {
      val repo = mockSessionRepoThatSaves(emptyUserAnswers)

      newService(repo).add(emptyUserAnswers).futureValue

      verify(repo).setPage(any(), eqTo(AllSuppliersQuery), eqTo(Map("1" -> Json.obj())))(any())
    }

    "must return the next number after the highest, even when there are gaps" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("2" -> answered, "5" -> answered))
      val repo    = mockSessionRepoThatSaves(answers)

      val number = newService(repo).add(answers).futureValue

      number mustBe SupplierNumber(6)
    }

    "must keep the existing suppliers when adding a new one" in {
      val existing = Map("2" -> answered)
      val answers  = emptyUserAnswers.unsafeSet(AllSuppliersQuery, existing)
      val repo     = mockSessionRepoThatSaves(answers)

      newService(repo).add(answers).futureValue

      verify(repo).setPage(any(), eqTo(AllSuppliersQuery), eqTo(existing + ("3" -> Json.obj())))(any())
    }

    "must reuse supplier 3 when it holds no values" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("2" -> answered, "3" -> Json.obj()))
      val repo    = mockSessionRepoThatSaves(answers)

      val number = newService(repo).add(answers).futureValue

      number mustBe SupplierNumber(3)
    }

    "must not return a deleted supplier's number again, must be next highest number" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("1" -> answered, "2" -> deleted))
      val repo    = mockSessionRepoThatSaves(answers)

      val number = newService(repo).add(answers).futureValue

      number mustBe SupplierNumber(3)
    }
  }

  "SupplierService.numberHasValues" - {

    "must be false for an empty supplier" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj()))

      newService(mock[SessionRepository]).numberHasValues(answers, SupplierNumber(1)) mustBe false
    }

    "must be true for a supplier holding an answer" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("1" -> answered))

      newService(mock[SessionRepository]).numberHasValues(answers, SupplierNumber(1)) mustBe true
    }
  }

  "SupplierService.deleteValues" - {

    "must empty supplier 1 and mark it deleted, keeping its number in the session" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("1" -> answered, "2" -> answered))

      val result = newService(mockSessionRepoThatSaves(answers), mockVehicleServiceThatRemoves(answers))
        .deleteValues(answers, SupplierNumber(1))
        .futureValue

      result.get(AllSuppliersQuery).value mustBe Map("1" -> deleted, "2" -> answered)
    }

    "must call VehicleService.deleteValuesForSupplier for supplier 1" in {
      val answers  = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("1" -> answered))
      val vehicles = mockVehicleServiceThatRemoves(answers)

      newService(mockSessionRepoThatSaves(answers), vehicles).deleteValues(answers, SupplierNumber(1)).futureValue

      verify(vehicles).deleteValuesForSupplier(any(), eqTo(SupplierNumber(1)))
    }

    "must empty the supplier and its vehicles in one write" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("1" -> answered))
      val repo    = mockSessionRepoThatSaves(answers)

      newService(repo, mockVehicleServiceThatRemoves(answers)).deleteValues(answers, SupplierNumber(1)).futureValue

      verify(repo, times(1)).set(any())
      verify(repo, never).setPage(any(), any(), any())(any())
    }

    "must leave suppliers 1 and 2 unchanged when supplier 9 is not in the session" in {
      val existing = Map("1" -> answered, "2" -> answered)
      val answers  = emptyUserAnswers.unsafeSet(AllSuppliersQuery, existing)

      val result = newService(mockSessionRepoThatSaves(answers), mockVehicleServiceThatRemoves(answers))
        .deleteValues(answers, SupplierNumber(9))
        .futureValue

      result.get(AllSuppliersQuery).value mustBe existing
    }
  }

  "SupplierService.inOrder" - {

    "must sort 10 after 2" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("10" -> answered, "2" -> answered))

      newService(mock[SessionRepository]).inOrder(answers).map { case (number, _) => number } mustBe
        Seq(SupplierNumber(2), SupplierNumber(10))
    }

    "must return nothing when there are no suppliers" in {
      newService(mock[SessionRepository]).inOrder(emptyUserAnswers) mustBe empty
    }

    "must ignore deleted and empty suppliers" in {
      val answers = emptyUserAnswers
        .unsafeSet(AllSuppliersQuery, Map("1" -> answered, "2" -> deleted, "3" -> Json.obj()))

      newService(mock[SessionRepository]).inOrder(answers).map { case (number, _) => number } mustBe
        Seq(SupplierNumber(1))
    }
  }

  "SupplierService.numberExists" - {

    "must return true for a supplier that exists" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("2" -> Json.obj(), "5" -> Json.obj()))

      newService(mock[SessionRepository]).numberExists(answers, SupplierNumber(5)) mustBe true
    }

    "must return false for a supplier that does not exist" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("2" -> Json.obj(), "5" -> Json.obj()))

      newService(mock[SessionRepository]).numberExists(answers, SupplierNumber(3)) mustBe false
    }

    "must return false when there are no suppliers" in {
      newService(mock[SessionRepository]).numberExists(emptyUserAnswers, SupplierNumber(1)) mustBe false
    }

    "must return false when supplier 3 is marked deleted" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("3" -> Json.obj("deleted" -> true)))

      newService(mock[SessionRepository]).numberExists(answers, SupplierNumber(3)) mustBe false
    }
  }

  "SupplierService.supplierName" - {

    "must return the purchaser's name when using purchaser details as the supplier" in {
      val answers = emptyUserAnswers
        .unsafeSet(UsePurchaserDetailsAsSupplierPage(supplierNumber), true)
        .unsafeSet(PurchaserNamePage, NameDetails("Mr", "firstName", "lastName"))

      val result = newService(mock[SessionRepository])
        .supplierName(answers, userContextFor(NovaUserType.PrivateIndividual), supplierNumber)
        .futureValue

      result mustBe Some("Mr firstName lastName")
    }

    "must return the purchaser's business name when using purchaser details as the supplier" in {
      val answers = emptyUserAnswers
        .unsafeSet(UsePurchaserDetailsAsSupplierPage(supplierNumber), true)
        .unsafeSet(PurchaserBusinessNamePage, "Test Co")

      val result = newService(mock[SessionRepository])
        .supplierName(answers, userContextFor(NovaUserType.PrivateIndividual), supplierNumber)
        .futureValue

      result mustBe Some("Test Co")
    }

    "must return the business name entered for a business supplier" in {
      val answers = emptyUserAnswers
        .unsafeSet(UsePersonalDetailsAsSupplierPage(supplierNumber), false)
        .unsafeSet(SupplierBusinessOrIndividualPage(supplierNumber), BusinessOrPrivateIndividual.Business)
        .unsafeSet(SupplierBusinessNamePage(supplierNumber), "Test Co")

      val result = newService(mock[SessionRepository])
        .supplierName(answers, userContextFor(NovaUserType.PrivateIndividual), supplierNumber)
        .futureValue

      result mustBe Some("Test Co")
    }

    "must return the supplier's personal name for a private individual supplier" in {
      val answers = emptyUserAnswers
        .unsafeSet(UsePersonalDetailsAsSupplierPage(supplierNumber), false)
        .unsafeSet(SupplierBusinessOrIndividualPage(supplierNumber), BusinessOrPrivateIndividual.PrivateIndividual)
        .unsafeSet(SupplierNamePage(supplierNumber), NameDetails("Mr", "firstName", "lastName"))

      val result = newService(mock[SessionRepository])
        .supplierName(answers, userContextFor(NovaUserType.PrivateIndividual), supplierNumber)
        .futureValue

      result mustBe Some("Mr firstName lastName")
    }

    "must return the notifier's business name when a business uses its own details as the supplier" in {
      val answers = emptyUserAnswers
        .unsafeSet(UsePersonalDetailsAsSupplierPage(supplierNumber), true)
        .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
        .unsafeSet(BusinessNamePage, "Test Co")

      val result = newService(mock[SessionRepository])
        .supplierName(answers, userContextFor(NovaUserType.NonVatOrganisation), supplierNumber)
        .futureValue

      result mustBe Some("Test Co")
    }

    "must return the notifier's own name when a private individual uses their own details as the supplier" in {
      val answers = emptyUserAnswers
        .unsafeSet(UsePersonalDetailsAsSupplierPage(supplierNumber), true)
        .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
        .unsafeSet(NameDetailsPage, NameDetails("Mr", "firstName", "lastName"))

      val result = newService(mock[SessionRepository])
        .supplierName(answers, userContextFor(NovaUserType.PrivateIndividual), supplierNumber)
        .futureValue

      result mustBe Some("Mr firstName lastName")
    }

    "for a VAT registered organisation using their own details as the supplier" - {

      val usingOwnDetails = emptyUserAnswers.unsafeSet(UsePersonalDetailsAsSupplierPage(supplierNumber), true)

      "must return the trader name when the vehicle is for business use" in {
        val result = newService(mock[SessionRepository], connector = connectorReturning(Right(traderInformation)))
          .supplierName(usingOwnDetails, userContextFor(NovaUserType.VatRegisteredOrganisation, isForBusinessUse = true), supplierNumber)
          .futureValue

        result mustBe Some("Test Co")
      }

      "must return None when the trader lookup finds no record" in {
        val result = newService(mock[SessionRepository], connector = connectorReturning(Left(GetTraderInformationError.NotFound)))
          .supplierName(usingOwnDetails, userContextFor(NovaUserType.VatRegisteredOrganisation, isForBusinessUse = true), supplierNumber)
          .futureValue

        result mustBe None
      }

      "must return None when the trader lookup throws" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.getTraderInformation()(any())).thenReturn(Future.failed(new RuntimeException("connection reset")))

        val result = newService(mock[SessionRepository], connector = connector)
          .supplierName(usingOwnDetails, userContextFor(NovaUserType.VatRegisteredOrganisation, isForBusinessUse = true), supplierNumber)
          .futureValue

        result mustBe None
      }

      "must return the name from the notifier's details when the vehicle is not for business use" in {
        val answers   = usingOwnDetails.unsafeSet(NameDetailsPage, NameDetails("Mr", "firstName", "lastName"))
        val connector = connectorReturning(Right(traderInformation))

        val result = newService(mock[SessionRepository], connector = connector)
          .supplierName(answers, userContextFor(NovaUserType.VatRegisteredOrganisation), supplierNumber)
          .futureValue

        result mustBe Some("Mr firstName lastName")
        verify(connector, never).getTraderInformation()(any())
      }
    }
  }
}
