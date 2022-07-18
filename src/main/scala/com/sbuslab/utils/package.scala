package com.sbuslab

import scala.concurrent.Future

import com.sbuslab.model.scheduler.ScheduleCommand
import com.sbuslab.sbus.Sbus


package object utils {

  implicit class SchedulerSupport(sbus: Sbus) {

    def schedule(routingKey: String, period: Long, body: Any = null, scheduleId: String = null, atTime: Long = 0L): Future[Unit] = {
      val context = sbus.sign(routingKey, body)

      sbus.command("scheduler.schedule", ScheduleCommand.builder
        .scheduleId(if (scheduleId != null) scheduleId else routingKey)
        .atTime(if (atTime != 0L) atTime else System.currentTimeMillis)
        .period(period)
        .routingKey(routingKey)
        .body(body)
        .origin(context.origin)
        .signature(context.signature)
        .build)
    }
  }
}
