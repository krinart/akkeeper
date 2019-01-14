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
package akkeeper.config

import java.time.{Duration => JavaDuration}
import java.util.concurrent.TimeUnit

import akka.util.Timeout
import akkeeper.common.ContainerDefinition
import akkeeper.config.ConfigUtils._
import akkeeper.app.storage.zookeeper.ZookeeperClientConfig
import com.typesafe.config.Config
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

private[akkeeper] final class AkkeeperConfig(akkeeperConfig: Config) {
  lazy val containers: Seq[ContainerDefinition] = {
    if (akkeeperConfig.hasPath("containers")) {
      val configContainers = akkeeperConfig.getConfigList("containers").asScala
      configContainers.map(ContainerDefinition.fromConfig)
    } else {
      Seq.empty
    }
  }
}

private[akkeeper] final class AkkeeperAkkaConfig(akkeeperAkkaConfig: Config) {
  lazy val actorSystemName: String = akkeeperAkkaConfig.getString("system-name")
  lazy val port: Int = akkeeperAkkaConfig.getInt("port")
  lazy val seedNodesNum: Int = akkeeperAkkaConfig.getInt("seed-nodes-num")
  lazy val joinClusterTimeout: FiniteDuration = akkeeperAkkaConfig.getDuration("join-cluster-timeout")
  lazy val leaveClusterTimeout: FiniteDuration = akkeeperAkkaConfig.getDuration("leave-cluster-timeout")
}

private[akkeeper] final class MonitoringConfig(monitoringConfig: Config) {
  lazy val launchTimeout: FiniteDuration = monitoringConfig.getDuration("launch-timeout")
}

private[akkeeper] final class LauncherConfig(launcherConfig: Config) {
  lazy val timeout: Option[Duration] = {
    if (launcherConfig.hasPath("timeout")) {
      Some(launcherConfig.getDuration("timeout", TimeUnit.SECONDS).seconds)
    } else {
      None
    }
  }
}

private[akkeeper] final class YarnConfig(yarnConfig: Config) {
  lazy val applicationName: String = yarnConfig.getString("application-name")
  lazy val maxAttempts: Int = yarnConfig.getInt("max-attempts")
  lazy val clientThreads: Int = yarnConfig.getInt("client-threads")
  def stagingDirectory(conf: Configuration, appId: String): String = {
    val basePath =
      if (yarnConfig.hasPath("staging-directory")) {
        yarnConfig.getString("staging-directory")
      } else {
        new Path(FileSystem.get(conf).getHomeDirectory, ".akkeeper").toString
      }
    new Path(basePath, appId).toString
  }
  lazy val masterCpus: Int = yarnConfig.getInt("master.cpus")
  lazy val masterMemory: Int = yarnConfig.getInt("master.memory")
  lazy val masterJvmArgs: Seq[String] = yarnConfig.getStringList("master.jvm.args").asScala
}

private[akkeeper] final class ZookeeperConfig(zookeeperConfig: Config) {
  lazy val clientConfig: ZookeeperClientConfig = ZookeeperClientConfig.fromConfig(zookeeperConfig)
}

private[akkeeper] final class RestConfig(restConfig: Config) {
  lazy val port: Int = restConfig.getInt("port")
  lazy val portMaxAttempts: Int = restConfig.getInt("port-max-attempts")
  lazy val requestTimeout: Timeout = Timeout(
    restConfig.getDuration("request-timeout", TimeUnit.MILLISECONDS),
    TimeUnit.MILLISECONDS)
}

private[akkeeper] final class KerberosConfig(kerberosConfig: Config) {
  lazy val ticketCheckInterval: Long =
    kerberosConfig.getDuration("ticket-check-interval", TimeUnit.MILLISECONDS)
}

object ConfigUtils {
  implicit def javaDuration2ScalaDuration(value: JavaDuration): FiniteDuration = {
    Duration.fromNanos(value.toNanos)
  }
}
