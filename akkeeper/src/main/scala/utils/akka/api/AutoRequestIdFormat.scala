/*
 * Copyright 2017-2018 Iaroslav Zeigerman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package utils.akka.api

import akkeeper.common._
import spray.json._
import utils.akka.RequestId

class AutoRequestIdFormat[T <: WithRequestId](original: JsonFormat[T])
  extends RootJsonFormat[T] {

  import utils.akka.RequestIdJsonProtocol._

  override def read(json: JsValue): T = {
    val jsObject = json.asJsObject
    val fields = jsObject.fields
    val updatedJsObject =
      if (!fields.contains("requestId")) {
        jsObject.copy(fields + ("requestId" -> RequestId().toJson))
      } else {
        jsObject
      }
    original.read(updatedJsObject)
  }

  override def write(obj: T): JsValue = original.write(obj)
}

object AutoRequestIdFormat {
  def apply[T <: WithRequestId](original: JsonFormat[T]): RootJsonFormat[T] = {
    new AutoRequestIdFormat(original)
  }
}
