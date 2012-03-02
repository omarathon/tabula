package uk.ac.warwick.courses

import java.util.Properties
import scala.collection.JavaConversions._
import scala.reflect.BeanInfo
import org.springframework.beans.BeanWrapperImpl
import org.springframework.core.env.PropertySource
import scala.reflect.BeanProperty
import java.lang.Boolean

/**
 * Defines flags to turn features on and off.
 * 
 * Defaults set in default.properties.
 * App can change startup features in its courses.properties,
 *   then modify them at runtime via JMX.
 */
case class Features(properties:Properties) {
	
	//// Features /////
	
	@BeanProperty var emailStudents:Boolean = false
	@BeanProperty var collectRatings:Boolean = true
	@BeanProperty var submissions:Boolean = false
	
	//// END of features ///
	
	
	// begin black magic that converts features.* properties into values
	// to inject into this instance.
	
	val featuresPrefix = "features."
	
	val bean = new BeanWrapperImpl(this)
	private def featureKeys = properties.keysIterator.filter( _ startsWith featuresPrefix )
	def capitalise(string:String) = string.head.toUpper + string.tail
	def removePrefix(string:String) = string.substring(featuresPrefix.length)
	def camelise(string:String) = removePrefix(string).split("\\.").toList match {
		case Nil => ""
		case head :: tail => head + tail.map(capitalise).mkString("")
	}
	
	for (key <- featureKeys) bean.setPropertyValue(camelise(key), properties.getProperty(key))
}

object Features {
	def empty = new Features(new Properties)
}
