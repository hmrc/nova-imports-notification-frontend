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

package controllers.actions

import base.SpecBase
import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase {

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Results.Ok)
  }

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new StandardIdentifierAction(new FakeFailingAuthConnector(new MissingBearerToken), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new StandardIdentifierAction(new FakeFailingAuthConnector(new BearerTokenExpired), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user doesn't have sufficient enrolments" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new StandardIdentifierAction(new FakeFailingAuthConnector(new InsufficientEnrolments), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "the user doesn't have sufficient confidence level" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new StandardIdentifierAction(new FakeFailingAuthConnector(new InsufficientConfidenceLevel), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "the user used an unaccepted auth provider" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new StandardIdentifierAction(new FakeFailingAuthConnector(new UnsupportedAuthProvider), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new StandardIdentifierAction(new FakeFailingAuthConnector(new UnsupportedAffinityGroup), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
        }
      }
    }

    "the user has an unsupported credential role" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new StandardIdentifierAction(new FakeFailingAuthConnector(new UnsupportedCredentialRole), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
        }
      }
    }
  }

  "PrivateIndividual Auth Action" - {

    "when the user is an Individual with no enrolments" - {
      "must execute the block and return OK" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val retrieval: Option[String] ~ Option[AffinityGroup] ~ Enrolments =
            new ~(new ~(Some("internal-id"), Some(AffinityGroup.Individual)), Enrolments(Set.empty))

          val authAction = new PrivateIndividualIdentifierAction(
            new FakeSuccessfulAuthConnector(retrieval),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
        }
      }
    }

    "when the user is an Individual with a VAT enrolment" - {
      "must execute the block and return OK" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers  = application.injector.instanceOf[BodyParsers.Default]
          val appConfig    = application.injector.instanceOf[FrontendAppConfig]
          val vatEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT").withIdentifier("VRN", "123456789")))
          val retrieval: Option[String] ~ Option[AffinityGroup] ~ Enrolments =
            new ~(new ~(Some("internal-id"), Some(AffinityGroup.Individual)), vatEnrolment)

          val authAction = new PrivateIndividualIdentifierAction(
            new FakeSuccessfulAuthConnector(retrieval),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
        }
      }
    }

    "when the user is an Organisation with no enrolments" - {
      "must execute the block and return OK" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers                                                    = application.injector.instanceOf[BodyParsers.Default]
          val appConfig                                                      = application.injector.instanceOf[FrontendAppConfig]
          val retrieval: Option[String] ~ Option[AffinityGroup] ~ Enrolments =
            new ~(new ~(Some("internal-id"), Some(AffinityGroup.Organisation)), Enrolments(Set.empty))

          val authAction = new PrivateIndividualIdentifierAction(
            new FakeSuccessfulAuthConnector(retrieval),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
        }
      }
    }

    "when the user is an Organisation with an inactive HMRC-MTD-VAT enrolment" - {
      "must execute the block and return OK" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers          = application.injector.instanceOf[BodyParsers.Default]
          val appConfig            = application.injector.instanceOf[FrontendAppConfig]
          val inactiveVatEnrolment =
            Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), state = "NotYetActivated")))
          val retrieval: Option[String] ~ Option[AffinityGroup] ~ Enrolments =
            new ~(new ~(Some("internal-id"), Some(AffinityGroup.Organisation)), inactiveVatEnrolment)

          val authAction = new PrivateIndividualIdentifierAction(
            new FakeSuccessfulAuthConnector(retrieval),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
        }
      }
    }

    "when the user is an Organisation with an active HMRC-MTD-VAT enrolment" - {
      "must redirect the user to Unauthorised" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers  = application.injector.instanceOf[BodyParsers.Default]
          val appConfig    = application.injector.instanceOf[FrontendAppConfig]
          val vatEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT").withIdentifier("VRN", "123456789")))
          val retrieval: Option[String] ~ Option[AffinityGroup] ~ Enrolments =
            new ~(new ~(Some("internal-id"), Some(AffinityGroup.Organisation)), vatEnrolment)

          val authAction = new PrivateIndividualIdentifierAction(
            new FakeSuccessfulAuthConnector(retrieval),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "when the user is an Organisation with an active HMCE-VATDEC-ORG enrolment" - {
      "must redirect the user to Unauthorised" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers     = application.injector.instanceOf[BodyParsers.Default]
          val appConfig       = application.injector.instanceOf[FrontendAppConfig]
          val vatDecEnrolment = Enrolments(Set(Enrolment("HMCE-VATDEC-ORG").withIdentifier("VATRegNo", "123456789")))
          val retrieval: Option[String] ~ Option[AffinityGroup] ~ Enrolments =
            new ~(new ~(Some("internal-id"), Some(AffinityGroup.Organisation)), vatDecEnrolment)

          val authAction = new PrivateIndividualIdentifierAction(
            new FakeSuccessfulAuthConnector(retrieval),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "when the user is an Agent with no enrolments" - {
      "must redirect the user to Unauthorised" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers                                                    = application.injector.instanceOf[BodyParsers.Default]
          val appConfig                                                      = application.injector.instanceOf[FrontendAppConfig]
          val retrieval: Option[String] ~ Option[AffinityGroup] ~ Enrolments =
            new ~(new ~(Some("internal-id"), Some(AffinityGroup.Agent)), Enrolments(Set.empty))

          val authAction = new PrivateIndividualIdentifierAction(
            new FakeSuccessfulAuthConnector(retrieval),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "when the user is an Agent with enrolments" - {
      "must redirect the user to Unauthorised" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers    = application.injector.instanceOf[BodyParsers.Default]
          val appConfig      = application.injector.instanceOf[FrontendAppConfig]
          val agentEnrolment = Enrolments(Set(Enrolment("HMCE-VAT-AGNT").withIdentifier("AgentRefNo", "999")))
          val retrieval: Option[String] ~ Option[AffinityGroup] ~ Enrolments =
            new ~(new ~(Some("internal-id"), Some(AffinityGroup.Agent)), agentEnrolment)

          val authAction = new PrivateIndividualIdentifierAction(
            new FakeSuccessfulAuthConnector(retrieval),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "when the internalId is missing" - {
      "must throw an UnauthorizedException" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers                                                    = application.injector.instanceOf[BodyParsers.Default]
          val appConfig                                                      = application.injector.instanceOf[FrontendAppConfig]
          val retrieval: Option[String] ~ Option[AffinityGroup] ~ Enrolments =
            new ~(new ~(None, Some(AffinityGroup.Individual)), Enrolments(Set.empty))

          val authAction = new PrivateIndividualIdentifierAction(
            new FakeSuccessfulAuthConnector(retrieval),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          whenReady(result.failed) { ex =>
            ex mustBe a[UnauthorizedException]
          }
        }
      }
    }

    "when there is no active session" - {
      "must redirect the user to log in" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new PrivateIndividualIdentifierAction(new FakeFailingAuthConnector(new MissingBearerToken), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "when an AuthorisationException is thrown" - {
      "must redirect the user to Unauthorised" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new PrivateIndividualIdentifierAction(new FakeFailingAuthConnector(new InsufficientEnrolments), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }
  }
}

class FakeFailingAuthConnector @Inject() (exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}

class FakeSuccessfulAuthConnector[T] @Inject() (valueToReturn: T) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.successful(valueToReturn.asInstanceOf[A])
}
