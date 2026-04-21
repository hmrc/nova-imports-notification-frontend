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

package config

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import controllers.actions.*

import java.time.{Clock, ZoneOffset}

class Module extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[DataRetrievalAction]).to(classOf[DataRetrievalActionImpl]).asEagerSingleton()
    bind(classOf[DataRequiredAction]).to(classOf[DataRequiredActionImpl]).asEagerSingleton()

    // Default binding — standard auth (any Individual/Organisation/Agent)
    bind(classOf[IdentifierAction]).to(classOf[StandardIdentifierAction]).asEagerSingleton()

    // @Named bindings for controllers that need specific auth levels
    bind(classOf[IdentifierAction])
      .annotatedWith(Names.named("standard"))
      .to(classOf[StandardIdentifierAction])
      .asEagerSingleton()
    bind(classOf[IdentifierAction])
      .annotatedWith(Names.named("vatTrader"))
      .to(classOf[VatTraderIdentifierAction])
      .asEagerSingleton()
    bind(classOf[IdentifierAction])
      .annotatedWith(Names.named("vatAgent"))
      .to(classOf[VatAgentIdentifierAction])
      .asEagerSingleton()
    bind(classOf[IdentifierAction])
      .annotatedWith(Names.named("ogd"))
      .to(classOf[OgdIdentifierAction])
      .asEagerSingleton()

    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC))
  }
}
