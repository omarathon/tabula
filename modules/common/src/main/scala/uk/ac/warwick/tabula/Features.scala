package uk.ac.warwick.tabula

import java.util.Properties
import scala.collection.JavaConversions._
import org.springframework.beans.BeanWrapperImpl
import scala.reflect.BeanProperty
import java.lang.Boolean


/**
 * Defines flags to turn features on and off.
 *
 * Defaults set in this class.
 * App can change startup features in its `tabula.properties`,
 *   then modify them at runtime via JMX.
 *
 * ==Adding a new feature==
 *
 * Define a new boolean variable here (with `@BeanProperty` so that it's
 * a valid JavaBean property), and then to set it to a different value in
 * `tabula.properties` add a line such as 
 *
 * {{{
 * features.yourFeatureName=false
 * }}}
 */
abstract class Features {

	@BeanProperty var emailStudents: Boolean = false
	@BeanProperty var collectRatings: Boolean = true
	@BeanProperty var submissions: Boolean = true
	@BeanProperty var privacyStatement: Boolean = true
	@BeanProperty var collectMarks: Boolean = true
	@BeanProperty var turnitin: Boolean = true
	@BeanProperty var assignmentMembership: Boolean = true
	@BeanProperty var extensions: Boolean = true
	@BeanProperty var combinedForm: Boolean = true	
	@BeanProperty var feedbackTemplates: Boolean = true
	@BeanProperty var markSchemes: Boolean = false
}

class FeaturesImpl(properties: Properties) extends Features {
	// begin black magic that converts features.* properties into values
	// to inject into this instance.

	private val featuresPrefix = "features."

	private val bean = new BeanWrapperImpl(this)
	private def featureKeys = properties.keysIterator.filter(_ startsWith featuresPrefix)
	def capitalise(string: String) = string.head.toUpper + string.tail
	def removePrefix(string: String) = string.substring(featuresPrefix.length)
	def camelise(string: String) = removePrefix(string).split("\\.").toList match {
		case Nil => ""
		case head :: tail => head + tail.map(capitalise).mkString("")
	}

	for (key <- featureKeys) bean.setPropertyValue(camelise(key), properties.getProperty(key))
}

object Features {
	def empty = new FeaturesImpl(new Properties)
	def fromProperties(p: Properties) = new FeaturesImpl(p)
}
