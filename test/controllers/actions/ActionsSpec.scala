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
import models.requests.DataRequest
import play.api.Application
import play.api.mvc.{AnyContent, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class ActionsSpec extends SpecBase {

  "Actions" - {

    "authAndGetData" - {

      "must execute the block when user data exists" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.authAndGetData()((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual OK
          contentAsString(result) mustEqual "success"
        }
      }

      "must redirect to Journey Recovery when no user data exists" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.authAndGetData()((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "privateIndividualAuthAndGetData" - {

      "must execute the block when user data exists" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.privateIndividualAuthAndGetData()((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual OK
          contentAsString(result) mustEqual "success"
        }
      }

      "must redirect to Journey Recovery when no user data exists" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.privateIndividualAuthAndGetData()((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "vatTraderAuthAndGetData" - {

      "must execute the block when user data exists" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.vatTraderAuthAndGetData()((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual OK
          contentAsString(result) mustEqual "success"
        }
      }

      "must redirect to Journey Recovery when no user data exists" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.vatTraderAuthAndGetData()((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "ogdAuthAndGetData" - {

      "must execute the block when user data exists" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.ogdAuthAndGetData()((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual OK
          contentAsString(result) mustEqual "success"
        }
      }

      "must redirect to Journey Recovery when no user data exists" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.ogdAuthAndGetData()((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "authAndGetDataWithGuard" - {

      "must execute the block when user data exists and predicate passes" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.authAndGetDataWithGuard(_ => true)((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual OK
          contentAsString(result) mustEqual "success"
        }
      }

      "must redirect to Journey Recovery when predicate fails" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.authAndGetDataWithGuard(_ => false)((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when no user data exists" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.authAndGetDataWithGuard(_ => true)((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "vatTraderAuthAndGetDataWithGuard" - {

      "must execute the block when user data exists and predicate passes" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.vatTraderAuthAndGetDataWithGuard(_ => true)((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual OK
          contentAsString(result) mustEqual "success"
        }
      }

      "must redirect to Journey Recovery when predicate fails" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.vatTraderAuthAndGetDataWithGuard(_ => false)((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "ogdAuthAndGetDataWithGuard" - {

      "must execute the block when user data exists and predicate passes" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.ogdAuthAndGetDataWithGuard(_ => true)((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual OK
          contentAsString(result) mustEqual "success"
        }
      }

      "must redirect to Journey Recovery when predicate fails" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val actions: Actions = application.injector.instanceOf[Actions]
          val action           = actions.ogdAuthAndGetDataWithGuard(_ => false)((_: DataRequest[AnyContent]) => Results.Ok("success"))
          val result           = action.apply(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
