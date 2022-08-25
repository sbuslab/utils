package com.sbuslab.utils

import scala.concurrent.Future

import com.sbuslab.model.Message
import com.sbuslab.model.scheduler.ScheduleCommand
import com.sbuslab.sbus.Sbus

trait SchedulerSupport {

  implicit class SbusSchedulerSupport(sbus: Sbus) {

    def schedule(
      routingKey: String,
      period: java.lang.Long = null,
      body: Any              = null,
      scheduleId: String     = null,
      atTime: java.lang.Long = null): Future[Unit] = {
      val context = sbus.sign(routingKey, new Message(routingKey, body))

      sbus.command(
        "scheduler.schedule",
        ScheduleCommand.builder
          .scheduleId(scheduleId)
          .atTime(atTime)
          .period(period)
          .routingKey(routingKey)
          .body(body)
          .origin(context.origin)
          .signature(context.signature)
          .build
      )
    }
  }
}
