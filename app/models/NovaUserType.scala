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

package models

import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}

enum NovaUserType {
  case VatRegisteredOrganisation
  case NonVatOrganisation
  case PrivateIndividual
  case Agent
}

object NovaUserType {

  def from(affinityGroup: AffinityGroup, enrolments: Enrolments): NovaUserType =
    affinityGroup match {
      case AffinityGroup.Organisation if hasVatEnrolment(enrolments) => NovaUserType.VatRegisteredOrganisation
      case AffinityGroup.Organisation                                => NovaUserType.NonVatOrganisation
      case AffinityGroup.Agent                                       => NovaUserType.Agent
      case _                                                         => NovaUserType.PrivateIndividual
    }

  private def hasVatEnrolment(enrolments: Enrolments): Boolean =
    enrolments.getEnrolment("HMRC-MTD-VAT").exists(_.isActivated) ||
      enrolments.getEnrolment("HMCE-VATDEC-ORG").exists(_.isActivated)
}
