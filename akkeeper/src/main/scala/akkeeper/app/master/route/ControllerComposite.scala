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
package akkeeper.app.master.route

import akka.http.scaladsl.server.{PathMatchers, Route}
import ControllerComposite._

case class ControllerComposite(basePath: String,
                               controllers: Seq[BaseController]) extends BaseController {
  override val route: Route = pathPrefix(PathMatchers.separateOnSlashes(basePath)) {
    controllers.reduceLeft((a, b) => UnitController(a.route ~ b.route)).route
  }
}

object ControllerComposite {
  private case class UnitController(override val route: Route) extends BaseController
}
