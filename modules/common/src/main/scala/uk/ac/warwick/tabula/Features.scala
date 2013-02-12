package uk.ac.warwick.tabula

import scala.reflect.BeanProperty

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
	
	@Value("${features.emailStudents:false}") @BeanProperty var emailStudents: Boolean = defaults.emailStudents
	@Value("${features.collectRatings:true}") @BeanProperty var collectRatings: Boolean = defaults.collectRatings
	@Value("${features.submissions:true}") @BeanProperty var submissions: Boolean = defaults.submissions
	@Value("${features.privacyStatement:true}") @BeanProperty var privacyStatement: Boolean = defaults.privacyStatement
	@Value("${features.collectMarks:true}") @BeanProperty var collectMarks: Boolean = defaults.collectMarks
	@Value("${features.turnitin:true}") @BeanProperty var turnitin: Boolean = defaults.turnitin
	@Value("${features.assignmentMembership:true}") @BeanProperty var assignmentMembership: Boolean = defaults.assignmentMembership
	@Value("${features.extensions:true}") @BeanProperty var extensions: Boolean = defaults.extensions
	@Value("${features.combinedForm:true}") @BeanProperty var combinedForm: Boolean = defaults.combinedForm
	@Value("${features.feedbackTemplates:true}") @BeanProperty var feedbackTemplates: Boolean = defaults.feedbackTemplates
	@Value("${features.markingWorkflows:false}") @BeanProperty var markingWorkflows: Boolean = defaults.markingWorkflows
	@Value("${features.markerFeedback:false}") @BeanProperty var markerFeedback: Boolean = defaults.markerFeedback
	@Value("${features.profiles:true}") @BeanProperty var profiles: Boolean = defaults.profiles
	
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
	
	@BeanProperty var emailStudents = false
	@BeanProperty var collectRatings = true
	@BeanProperty var submissions = true
	@BeanProperty var privacyStatement = true
	@BeanProperty var collectMarks = true
	@BeanProperty var turnitin = true
	@BeanProperty var assignmentMembership = true
	@BeanProperty var extensions = true
	@BeanProperty var combinedForm = true
	@BeanProperty var feedbackTemplates = true
	@BeanProperty var markingWorkflows = true
	@BeanProperty var markerFeedback = false
	@BeanProperty var profiles = true
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
