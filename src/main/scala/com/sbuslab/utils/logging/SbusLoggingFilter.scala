package com.sbuslab.utils.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

class SbusLoggingFilter extends Filter[ILoggingEvent] {
  def decide(event: ILoggingEvent) =
    if (event.getMessage.contains("~~~")) FilterReply.ACCEPT else FilterReply.DENY
}
