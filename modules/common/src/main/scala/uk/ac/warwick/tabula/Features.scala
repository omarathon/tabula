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
	@Value("${features.emailStudents:false}") @BeanProperty var emailStudents: Boolean = _
	@Value("${features.collectRatings:true}") @BeanProperty var collectRatings: Boolean = _
	@Value("${features.submissions:true}") @BeanProperty var submissions: Boolean = _
	@Value("${features.privacyStatement:true}") @BeanProperty var privacyStatement: Boolean = _
	@Value("${features.collectMarks:true}") @BeanProperty var collectMarks: Boolean = _
	@Value("${features.turnitin:true}") @BeanProperty var turnitin: Boolean = _
	@Value("${features.assignmentMembership:true}") @BeanProperty var assignmentMembership: Boolean = _
	@Value("${features.extensions:true}") @BeanProperty var extensions: Boolean = _
	@Value("${features.combinedForm:true}") @BeanProperty var combinedForm: Boolean = _
	@Value("${features.feedbackTemplates:true}") @BeanProperty var feedbackTemplates: Boolean = _
	@Value("${features.markSchemes:false}") @BeanProperty var markSchemes: Boolean = _
}

class FeaturesImpl extends Features

object Features {
	def empty = new FeaturesImpl
}
