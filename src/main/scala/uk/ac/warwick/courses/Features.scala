package uk.ac.warwick.courses

import java.util.Properties
import scala.collection.JavaConversions._
import scala.reflect.BeanInfo
import org.springframework.beans.BeanWrapperImpl
import org.springframework.core.env.PropertySource
import scala.reflect.BeanProperty
import java.lang.Boolean
import scala.reflect.BeanInfo
import java.beans.SimpleBeanInfo
import java.beans.BeanDescriptor
import java.beans.PropertyDescriptor

/**
 * Defines flags to turn features on and off.
 * 
 * Defaults set in default.properties.
 * App can change startup features in its courses.properties,
 *   then modify them at runtime via JMX.
 */
abstract class Features {

	@BeanProperty var emailStudents:Boolean = false
	@BeanProperty var collectRatings:Boolean = true
	@BeanProperty var submissions:Boolean = false
	@BeanProperty var privacyStatement:Boolean = true
	@BeanProperty var collectMarks:Boolean = false
}


class FeaturesImpl(properties:Properties) extends Features {
	// begin black magic that converts features.* properties into values
	// to inject into this instance.
	
	private val featuresPrefix = "features."
	
	private val bean = new BeanWrapperImpl(this)
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
	def empty = new FeaturesImpl(new Properties)
	def fromProperties(p:Properties) = new FeaturesImpl(p)
}
