package com.sbuslab.utils

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory


trait Logging {

  protected val log = getLogger(this.getClass.getName)

  def getLogger(name: String) = Logger(LoggerFactory.getLogger(name))
}


object Logging extends Logging {
  override val log = getLogger(this.getClass.getName)
}
