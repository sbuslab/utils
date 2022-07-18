package com.sbuslab.utils

import com.sbuslab.model.scheduler.ScheduleCommand
import com.sbuslab.sbus.Sbus

trait Scheduling {


  def sbus: Sbus

  def schedule(routingKey: String, period: Long, body: Option[Any] = Option.empty, scheduleId: Option[String] = Option.empty, atTime: Option[Long] = Option.empty): Unit = {
    val context = sbus.sign(routingKey, body)

    sbus.command("scheduler.schedule", ScheduleCommand.builder
      .scheduleId(scheduleId.getOrElse(routingKey))
      .atTime(atTime.getOrElse(System.currentTimeMillis))
      .period(period)
      .routingKey(routingKey)
      .body(body.orNull)
      .origin(context.origin)
      .signature(context.signature)
      .build)
  }
}
