package com.sbuslab.utils

import com.typesafe.scalalogging.{CanLog, Logger}
import org.slf4j.{LoggerFactory, MDC}

import com.sbuslab.sbus.Context


trait Logging {

  implicit case object CanLogSbusContext extends CanLog[Context] {
    override def logMessage(originalMsg: String, context: Context): String = {
      MDC.put("correlation_id", context.correlationId)
      originalMsg
    }

    override def afterLog(context: Context): Unit = {
      MDC.remove("correlation_id")
    }
  }

  protected val log = getLogger(this.getClass.getName)

  protected val slog = Logger.takingImplicit[Context](LoggerFactory.getLogger(this.getClass.getName))

  def getLogger(name: String) = Logger(LoggerFactory.getLogger(name))
}


object Logging extends Logging {
  override val log = getLogger(this.getClass.getName)
}
