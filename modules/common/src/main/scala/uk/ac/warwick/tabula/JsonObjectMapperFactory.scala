package uk.ac.warwick.tabula
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.config.AbstractFactoryBean
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper

class JsonObjectMapperFactory extends AbstractFactoryBean[ObjectMapper] {
	override def getObjectType = classOf[ObjectMapper]
	override def createInstance = {
		val mapper = new ObjectMapper
		mapper.registerModule(DefaultScalaModule)
		mapper
	}
}