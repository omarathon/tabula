package uk.ac.warwick.tabula

import scala.beans.BeanProperty

import org.codehaus.jackson.annotate.JsonAutoDetect
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.util.queue.QueueListener
import uk.ac.warwick.util.queue.conversion.ItemType


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
	private val defaults = new FeaturesMessage
	
	// FIXME currently requires default to be set twice: in annotation for Spring, and in FeaturesMessage non-Spring tests
	
	@Value("${features.emailStudents:false}") var emailStudents: Boolean = defaults.emailStudents
	@Value("${features.collectRatings:true}") var collectRatings: Boolean = defaults.collectRatings
	@Value("${features.submissions:true}") var submissions: Boolean = defaults.submissions
	@Value("${features.privacyStatement:true}") var privacyStatement: Boolean = defaults.privacyStatement
	@Value("${features.collectMarks:true}") var collectMarks: Boolean = defaults.collectMarks
	@Value("${features.turnitin:true}") var turnitin: Boolean = defaults.turnitin
	@Value("${features.assignmentMembership:true}") var assignmentMembership: Boolean = defaults.assignmentMembership
	@Value("${features.extensions:true}") var extensions: Boolean = defaults.extensions
	@Value("${features.combinedForm:true}") var combinedForm: Boolean = defaults.combinedForm
	@Value("${features.feedbackTemplates:true}") var feedbackTemplates: Boolean = defaults.feedbackTemplates
	@Value("${features.markingWorkflows:true}") var markingWorkflows: Boolean = defaults.markingWorkflows
	@Value("${features.markerFeedback:true}") var markerFeedback: Boolean = defaults.markerFeedback
	@Value("${features.profiles:true}") var profiles: Boolean = defaults.profiles
	
	private val bean = new BeanWrapperImpl(this)
	def update(message: FeaturesMessage) = {
		val values = new BeanWrapperImpl(message)
		
		for (pd <- values.getPropertyDescriptors if bean.getPropertyDescriptor(pd.getName).getWriteMethod != null)
			bean.setPropertyValue(pd.getName, values.getPropertyValue(pd.getName))
		this
	}
}

class FeaturesImpl extends Features

@ItemType("Features")
@JsonAutoDetect
class FeaturesMessage {
	// Warning: If you make this more complicated, you may break the Jackson auto-JSON stuff for the FeaturesController
	
	def this(features: Features) {
		this()
		
		val bean = new BeanWrapperImpl(this)
		val values = new BeanWrapperImpl(features)
		
		for (pd <- bean.getPropertyDescriptors if bean.getPropertyDescriptor(pd.getName).getWriteMethod != null)
			bean.setPropertyValue(pd.getName, values.getPropertyValue(pd.getName))
	}
	
	var emailStudents = false
	var collectRatings = true
	var submissions = true
	var privacyStatement = true
	var collectMarks = true
	var turnitin = true
	var assignmentMembership = true
	var extensions = true
	var combinedForm = true
	var feedbackTemplates = true
	var markingWorkflows = true
	var markerFeedback = true
	var profiles = true
}

class FeatureFlagListener extends QueueListener with InitializingBean with Logging {
	
		var queue = Wire.named[Queue]("settingsSyncTopic")
		var features = Wire.auto[Features]
		
		override def isListeningToQueue = true
		override def onReceive(item: Any) {	
				item match {
						case copy: FeaturesMessage => features.update(copy)
				}
		}
		
		override def afterPropertiesSet() {
				queue.addListener(classOf[FeaturesMessage].getAnnotation(classOf[ItemType]).value, this)
		}
	
}

object Features {
	def empty = new FeaturesImpl
}
