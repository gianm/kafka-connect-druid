/*
 * Copyright 2016 Imply Data, Inc.
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

package io.imply.kafkaconnect.druid

import java.{util => ju}
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.sink.SinkConnector
import scala.collection.JavaConverters._

class TranquilitySinkConnector extends SinkConnector
{
  private var config: TranquilitySinkConfig = null

  override def start(props: ju.Map[String, String]): Unit = {
    config = new TranquilitySinkConfig(props.asScala.toMap)
  }

  override def stop(): Unit = {
    // Nothing to do
  }

  override def taskClass(): Class[_ <: Task] = classOf[TranquilitySinkTask]

  override def taskConfigs(maxTasks: Int): ju.List[ju.Map[String, String]] = {
    (for (i <- 0 until maxTasks) yield {
      config.props.asJava
    }).asJava
  }

  override def version(): String = {
    Option(getClass.getPackage.getImplementationVersion).getOrElse("")
  }
}
