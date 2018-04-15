package com.sbuslab.utils

import java.lang.reflect.{ParameterizedType, Type}

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import com.sbuslab.utils.json.JsonMapperFactory


trait JsonFormatter {

  def serialize(value: Any): String =
    JsonFormatter.mapper.writeValueAsString(value)

  def serializePretty(value: Any): String =
    JsonFormatter.mapper.writer.withDefaultPrettyPrinter().writeValueAsString(value)

  def deserialize[T: Manifest](value: String): T =
    JsonFormatter.mapper.readValue(value, typeReference[T])

  def deserialize[T: Manifest](value: Array[Byte]): T =
    deserialize[T](new String(value))

  def deserialize[T: Manifest](value: String, clazz: Class[T]): T =
    deserialize(value)

  def deserialize[T: Manifest](value: Array[Byte], clazz: Class[T]): T =
    deserialize(value)

  def typeReference[T: Manifest] = new TypeReference[T] {
    override def getType = typeFromManifest(manifest[T])
  }

  private def typeFromManifest(m: Manifest[_]): Type =
    if (m.typeArguments.isEmpty) {
      m.runtimeClass
    } else {
      new ParameterizedType {
        def getRawType = m.runtimeClass
        def getActualTypeArguments = m.typeArguments.map(typeFromManifest).toArray
        def getOwnerType = null
      }
    }
}


object JsonFormatter extends JsonFormatter {

  val mapper = createMapper()

  val factory = new MappingJsonFactory(mapper)

  def createMapper() = {
    val m = JsonMapperFactory.createMapper()
    m.registerModule(DefaultScalaModule)
    m
  }
}
