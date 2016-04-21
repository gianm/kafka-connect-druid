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

import com.metamx.common.scala.Jackson
import com.metamx.common.scala.collection.implicits._
import com.metamx.common.scala.untyped.Dict
import com.metamx.tranquility.config.DataSourceConfig
import com.metamx.tranquility.config.PropertiesBasedConfig
import com.metamx.tranquility.config.TranquilityConfig
import io.imply.kafkaconnect.druid.TranquilitySinkConfig._
import java.io.ByteArrayInputStream

class TranquilitySinkConfig(val props: Map[String, String])
{
  def specString: String = getRequiredProperty(SpecStringProperty)

  lazy val tranquilityConfig: DataSourceConfig[PropertiesBasedConfig] = {
    TranquilityConfig.read(
      new ByteArrayInputStream(
        Jackson.bytes(
          Dict(
            "properties" -> props,
            "dataSources" -> Seq(
              Dict(
                "spec" -> Jackson.parse[Dict](specString),
                "properties" -> Nil
              )
            )
          )
        )
      )
    ).dataSourceConfigs.values.onlyElement
  }

  private def getRequiredProperty(k: String): String = {
    props.getOrElse(k, throw new IllegalArgumentException(s"Missing required property '$k'"))
  }
}

object TranquilitySinkConfig
{
  val SpecStringProperty = "druid.specString"
}
