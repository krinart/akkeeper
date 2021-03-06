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
package akkeeper.common

import akka.actor.Address
import akka.cluster.UniqueAddress
import akkeeper.AkkeeperException
import spray.json._

/** Represents a status of the instance. */
sealed trait InstanceStatus {
  /** String representation of the status. */
  def value: String
}
case object InstanceDeploying extends InstanceStatus { val value = "DEPLOYING" }
case object InstanceDeployFailed extends InstanceStatus { val value = "DEPLOY_FAILED" }
case object InstanceLaunching extends InstanceStatus { val value = "LAUNCHING" }
case object InstanceUp extends InstanceStatus { val value = "UP" }
case object InstanceUnreachable extends InstanceStatus { val value = "UNREACHABLE" }
object InstanceStatus {
  def fromString(status: String): InstanceStatus = {
    status match {
      case InstanceDeploying.value => InstanceDeploying
      case InstanceDeployFailed.value => InstanceDeployFailed
      case InstanceLaunching.value => InstanceLaunching
      case InstanceUp.value => InstanceUp
      case InstanceUnreachable.value => InstanceUnreachable
      case other => throw new AkkeeperException(s"Unexpected instance status $other")
    }
  }
}

/** A complete information about the instance.
  *
  * @param instanceId the unique ID of the instance.
  * @param status the instance's status.
  * @param containerName the name of the container this instance belongs to.
  * @param roles the list of the instance's Akka roles.
  * @param address the address of the instance.
  * @param actors the list of user actors that are available on this instance.
  * @param extra the key value properties with additional instance specific data.
  */
case class InstanceInfo private[akkeeper] (instanceId: InstanceId, status: InstanceStatus,
                                           containerName: String, roles: Set[String],
                                           address: Option[UniqueAddress], actors: Set[String],
                                           extra: Map[String, String] = Map.empty)

object InstanceInfo {
  private def createWithStatus(instanceId: InstanceId, status: InstanceStatus): InstanceInfo = {
    InstanceInfo(instanceId, status, instanceId.containerName, Set.empty, None, Set.empty)
  }

  private[akkeeper] def deploying(instanceId: InstanceId): InstanceInfo = {
    createWithStatus(instanceId, InstanceDeploying)
  }

  private[akkeeper] def deployFailed(instanceId: InstanceId): InstanceInfo = {
    createWithStatus(instanceId, InstanceDeployFailed)
  }

  private[akkeeper] def launching(instanceId: InstanceId): InstanceInfo = {
    createWithStatus(instanceId, InstanceLaunching)
  }
}

trait InstanceStatusJsonProtocol extends DefaultJsonProtocol with InstanceIdJsonProtocol {

  implicit val addressFormat = new JsonFormat[Address] {
    override def write(obj: Address): JsValue = {
      val protocolField = JsString(obj.protocol)
      val systemField = JsString(obj.system)
      val hostField = obj.host.map(JsString(_))
      val portField = obj.port.map(JsNumber(_))

      JsObject(
        "protocol" -> protocolField,
        "system" -> systemField,
        "host" -> hostField.getOrElse(JsNull),
        "port" -> portField.getOrElse(JsNull)
      )
    }

    override def read(json: JsValue): Address = {
      val fields = json.asJsObject.fields
      val protocol = fields("protocol").convertTo[String]
      val system = fields("system").convertTo[String]
      val host = fields("host")
      val port = fields("port")
      if (host != JsNull && port != JsNull) {
        Address(protocol, system, host.convertTo[String], port.convertTo[Int])
      } else {
        Address(protocol, system)
      }
    }
  }

  implicit val uniqueAddressFormat =
    jsonFormat[Address, Long, UniqueAddress]((a, u) => UniqueAddress(a, u), "address", "longUid")

  implicit val instanceStatusFormat = new JsonFormat[InstanceStatus] {
    override def read(json: JsValue): InstanceStatus = {
      InstanceStatus.fromString(json.convertTo[String])
    }
    override def write(obj: InstanceStatus): JsValue = {
      JsString(obj.value)
    }
  }

  implicit val instanceInfoFormat = jsonFormat7(InstanceInfo.apply)
}

object InstanceStatusJsonProtocol extends InstanceStatusJsonProtocol
