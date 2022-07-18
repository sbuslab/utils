package com.sbuslab.utils

import com.sbuslab.model.scheduler.ScheduleCommand
import com.sbuslab.sbus.Sbus

import scala.concurrent.Future

trait Scheduling {


  def sbus: Sbus

  def schedule(scheduleCommand: ScheduleCommand): Future[Unit] = {
    val context = sbus.sign(scheduleCommand.getRoutingKey, scheduleCommand.getBody)

    sbus.command("scheduler.schedule", scheduleCommand.toBuilder
      .origin(context.origin)
      .signature(context.signature)
      .build)
  }
}
