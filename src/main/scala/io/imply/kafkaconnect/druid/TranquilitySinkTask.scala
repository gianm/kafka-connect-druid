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

import com.metamx.common.scala.Logging
import com.metamx.tranquility.druid.DruidBeams
import com.metamx.tranquility.tranquilizer.MessageDroppedException
import com.metamx.tranquility.tranquilizer.Tranquilizer
import com.twitter.util.Return
import com.twitter.util.Throw
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.{util => ju}
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.sink.SinkRecord
import org.apache.kafka.connect.sink.SinkTask
import scala.collection.JavaConverters._

class TranquilitySinkTask extends SinkTask with Logging
{
  private var sender: Tranquilizer[ju.Map[String, AnyRef]] = null

  private val received    = new AtomicLong
  private val sent        = new AtomicLong
  private val unparseable = new AtomicLong
  private val dropped     = new AtomicLong
  private val failed      = new AtomicLong
  private val exception   = new AtomicReference[Throwable](null)

  override def start(props: ju.Map[String, String]): Unit = {
    val config = new TranquilitySinkConfig(props.asScala.toMap)
    sender = DruidBeams.fromConfig(config.tranquilityConfig)
      .buildTranquilizer(config.tranquilityConfig.tranquilizerBuilder())
    sender.start()
  }

  override def put(records: ju.Collection[SinkRecord]): Unit = {
    for (record <- records.asScala) {
      received.incrementAndGet()

      val recordAsMap: Option[ju.Map[String, AnyRef]] = try {
        Some(TranquilitySinkTask.convert(record))
      }
      catch {
        case e: Exception =>
          log.debug(e, "Could not parse record")
          unparseable.incrementAndGet()
          None
      }

      if (recordAsMap.isDefined) {
        sender.send(recordAsMap.get) respond {
          case Return(_) => sent.incrementAndGet()
          case Throw(e: MessageDroppedException) => dropped.incrementAndGet()
          case Throw(e) =>
            failed.incrementAndGet()
            exception.compareAndSet(null, e)
        }
      }
    }

    maybeThrow()
  }

  override def flush(offsets: ju.Map[TopicPartition, OffsetAndMetadata]): Unit = {
    sender.flush()

    val receivedDelta = received.getAndSet(0)
    val sentDelta = sent.getAndSet(0)
    val unparseableDelta = unparseable.getAndSet(0)
    val droppedDelta = dropped.getAndSet(0)
    val failedDelta = failed.getAndSet(0)

    log.info(
      s"Received[$receivedDelta], sent[$sentDelta], ignored[${unparseableDelta + droppedDelta + failedDelta}]" +
        s" messages (unparseable[$unparseableDelta], dropped[$droppedDelta], failed[$failedDelta])"
    )

    maybeThrow()
  }

  override def stop(): Unit = {
    sender.stop()
  }

  override def version(): String = {
    Option(getClass.getPackage.getImplementationVersion).getOrElse("")
  }

  private def maybeThrow(): Unit = {
    if (exception.get() != null) {
      throw exception.get()
    }
  }
}

object TranquilitySinkTask
{
  def convert(sinkRecord: SinkRecord): ju.Map[String, AnyRef] = {
    val value = sinkRecord.value()
    val valueSchema = sinkRecord.valueSchema()

    val retVal: Map[String, Any] = valueSchema.`type`() match {
      case Schema.Type.MAP =>
        val valueAsMap = value.asInstanceOf[ju.Map[_, _]].asScala
        (for ((k, v) <- valueAsMap) yield {
          (String.valueOf(k), v)
        }).toMap

      case Schema.Type.STRUCT =>
        val valueAsStruct = value.asInstanceOf[Struct]
        (for (field <- valueSchema.fields().asScala) yield {
          (field.name(), valueAsStruct.get(field))
        }).toMap

      case _ =>
        throw new IllegalArgumentException(s"value must be Map or Struct, but was[${valueSchema.`type`}]")
    }

    retVal.asJava.asInstanceOf[ju.Map[String, AnyRef]]
  }
}
