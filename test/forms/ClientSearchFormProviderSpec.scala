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

package forms

import forms.behaviours.FieldBehaviours
import models.{ClientSearch, ClientSearchBy}
import play.api.data.FormError

class ClientSearchFormProviderSpec extends FieldBehaviours {

  val form = new ClientSearchFormProvider()()

  ".searchBy" - {

    behave like mandatoryField(form, "searchBy", requiredError = FormError("searchBy", "viewClients.searchBy.error.required"))

    "must not bind an unknown search type" in {
      form.bind(Map("searchBy" -> "email", "search" -> "x")).apply("searchBy").errors mustEqual
        Seq(FormError("searchBy", "viewClients.searchBy.error.required"))
    }
  }

  ".search" - {

    "must ask for a name or VRN when no search type is chosen" in {
      form.bind(Map("searchBy" -> "", "search" -> "")).apply("search").errors mustEqual
        Seq(FormError("search", "viewClients.search.error.required"))
    }

    "must ask for the client's name when searching by name" in {
      form.bind(Map("searchBy" -> "name", "search" -> " ")).apply("search").errors mustEqual
        Seq(FormError("search", "viewClients.search.error.required.name"))
    }

    "must ask for the client's VRN when searching by VRN" in {
      form.bind(Map("searchBy" -> "vrn")).apply("search").errors mustEqual
        Seq(FormError("search", "viewClients.search.error.required.vrn"))
    }
  }

  "must bind a trimmed search" in {
    form.bind(Map("searchBy" -> "name", "search" -> "  Client Co ")).value.value mustEqual ClientSearch(ClientSearchBy.Name, "Client Co")
  }

  "must fill both fields" in {
    form.fill(ClientSearch(ClientSearchBy.Vrn, "123456789")).data mustEqual Map("searchBy" -> "vrn", "search" -> "123456789")
  }
}
