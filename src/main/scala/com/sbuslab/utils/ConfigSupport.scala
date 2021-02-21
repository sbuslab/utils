package com.sbuslab.utils

import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import scala.concurrent.duration.{FiniteDuration, _}

import com.typesafe.config.{Config, ConfigValue}


trait ConfigSupport {

  implicit class EnhancedConfig(val config: Config) {

    def optBoolean(path: String)        = opt(path, _.getBoolean)
    def optString(path: String)         = opt(path, _.getString)
    def optInt(path: String)            = opt(path, _.getInt)
    def optLong(path: String)           = opt(path, _.getLong)
    def optObject(path: String)         = opt(path, _.getObject)
    def optFiniteDuration(path: String) = opt(path, _ ⇒ getFiniteDuration)
    def optStringList(path: String)     = opt(path, _.getStringList)
    def optConfigList(path: String)     = opt(path, _.getConfigList)

    def getConfigMap(path: String): Map[String, Config] = getMap(path).mapValues(_.atPath("/").getConfig("/"))
    def getStringMap(path: String): Map[String, String] = getMap(path).mapValues(_.atPath("/").getString("/"))

    def getFiniteDuration(path: String): FiniteDuration = config.getDuration(path, TimeUnit.MILLISECONDS).millis

    def getMap(path: String): Map[String, ConfigValue] =
      if (config.hasPath(path)) {
        config.getObject(path).entrySet().asScala.map(entry ⇒ entry.getKey → entry.getValue).toMap
      } else {
        Map.empty
      }

    private def opt[T](path: String, getter: Config ⇒ String ⇒ T): Option[T] =
      if (config.hasPath(path)) {
        Option(getter(config)(path))
      } else {
        None
      }
  }
}
