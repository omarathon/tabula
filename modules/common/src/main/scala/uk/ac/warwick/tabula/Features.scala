package uk.ac.warwick.tabula

import java.util.Properties
import scala.collection.JavaConversions._
import org.springframework.beans.BeanWrapperImpl
import scala.reflect.BeanProperty
import java.lang.Boolean
import org.springframework.beans.factory.annotation.Value


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
	// FIXME currently requires default to be set twice: in annotation for Spring, and at the end for non-Spring tests
	@Value("${features.emailStudents:false}") @BeanProperty var emailStudents: Boolean = false
	@Value("${features.collectRatings:true}") @BeanProperty var collectRatings: Boolean = true
	@Value("${features.submissions:true}") @BeanProperty var submissions: Boolean = true
	@Value("${features.privacyStatement:true}") @BeanProperty var privacyStatement: Boolean = true
	@Value("${features.collectMarks:true}") @BeanProperty var collectMarks: Boolean = true
	@Value("${features.turnitin:true}") @BeanProperty var turnitin: Boolean = true
	@Value("${features.assignmentMembership:true}") @BeanProperty var assignmentMembership: Boolean = true
	@Value("${features.extensions:true}") @BeanProperty var extensions: Boolean = true
	@Value("${features.combinedForm:true}") @BeanProperty var combinedForm: Boolean = true
	@Value("${features.feedbackTemplates:true}") @BeanProperty var feedbackTemplates: Boolean = true
	@Value("${features.markSchemes:false}") @BeanProperty var markSchemes: Boolean = false
	@Value("${features.profiles:false}") @BeanProperty var profiles: Boolean = false
}

class FeaturesImpl extends Features

object Features {
	def empty = new FeaturesImpl
}
