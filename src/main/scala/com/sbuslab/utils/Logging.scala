package com.sbuslab.utils

import com.typesafe.scalalogging.{CanLog, Logger, LoggerTakingImplicit}
import org.slf4j.{LoggerFactory, MDC}

import com.sbuslab.sbus.Context


trait Logging {

  implicit case object CanLogSbusContext extends CanLog[Context] {
    override def logMessage(originalMsg: String, context: Context): String = {
      val fields = context.customData

      if (fields.nonEmpty) {
        MDC.put("meta", JsonFormatter.serialize(fields))
      }

      MDC.put("correlation_id", context.correlationId)
      originalMsg
    }

    override def afterLog(context: Context): Unit = {
      MDC.remove("meta")
      MDC.remove("correlation_id")
    }
  }

  implicit class CriticalLogger(underlying: LoggerTakingImplicit[Context]) {
    def critical(message: String)(implicit ctx: Context): Unit = underlying.error("[Critical Alert] " + message)
    def critical(message: String, cause: Throwable)(implicit ctx: Context): Unit = underlying.error("[Critical Alert] " + message, cause)
    def critical(message: String, args: Any*)(implicit ctx: Context): Unit = underlying.error("[Critical Alert] " + message, args)
  }

  protected val log: Logger =
    getLogger(this.getClass.getName)

  protected val slog: LoggerTakingImplicit[Context] =
    Logger.takingImplicit[Context](LoggerFactory.getLogger(this.getClass.getName))

  def getLogger(name: String) = Logger(LoggerFactory.getLogger(name))
}


object Logging extends Logging {
  override val log = getLogger(this.getClass.getName)
}
