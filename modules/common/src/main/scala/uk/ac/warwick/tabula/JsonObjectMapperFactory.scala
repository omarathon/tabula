package uk.ac.warwick.tabula
import scala.reflect._

import org.springframework.beans.factory.config.AbstractFactoryBean
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule

abstract class ScalaFactoryBean[A : ClassTag] extends AbstractFactoryBean[A] {
	override def getObjectType = classTag[A].runtimeClass
}

class JsonObjectMapperFactory extends ScalaFactoryBean[ObjectMapper] {
	override def createInstance: ObjectMapper = JsonObjectMapperFactory.createInstance
}

object JsonObjectMapperFactory  {
	val instance: ObjectMapper = createInstance
	def createInstance = {
		val mapper = new ObjectMapper
		mapper.registerModule(DefaultScalaModule)
		mapper.registerModule(new JodaModule)
		mapper
	}
}
