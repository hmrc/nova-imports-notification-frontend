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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.PathBindable

class ImportNumberSpec extends AnyFreeSpec with Matchers {

  private val binder: PathBindable[ImportNumber] = implicitly[PathBindable[ImportNumber]]

  "ImportNumber.pathBindable" - {

    "must bind a URL value to the same import number" in {
      binder.bind("importNumber", "1") mustBe Right(ImportNumber(1))
      binder.bind("importNumber", "2") mustBe Right(ImportNumber(2))
    }

    "must reject zero, negative and non-numeric values" in {
      binder.bind("importNumber", "0").isLeft mustBe true
      binder.bind("importNumber", "-1").isLeft mustBe true
      binder.bind("importNumber", "abc").isLeft mustBe true
    }

    "must unbind an import number to the same URL value" in {
      binder.unbind("importNumber", ImportNumber(1)) mustBe "1"
      binder.unbind("importNumber", ImportNumber(3)) mustBe "3"
    }
  }
}
