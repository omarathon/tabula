package uk.ac.warwick.tabula


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
 * Define a new boolean variable here and FeaturesMessages, and then to set it to a different value in
 * `tabula.properties` add a line such as 
 *
 * {{{
 * features.yourFeatureName=false
 * }}}
 */
abstract class Features {
	private val defaults = new FeaturesMessage
	
	// FIXME currently requires default to be set twice: in annotation for Spring, and in FeaturesMessage non-Spring tests
	
	@Value("${features.emailStudents:false}") var emailStudents = defaults.emailStudents
	@Value("${features.collectRatings:true}") var collectRatings = defaults.collectRatings
	@Value("${features.submissions:true}") var submissions = defaults.submissions
	@Value("${features.privacyStatement:true}") var privacyStatement = defaults.privacyStatement
	@Value("${features.collectMarks:true}") var collectMarks = defaults.collectMarks
	@Value("${features.turnitin:true}") var turnitin = defaults.turnitin
	@Value("${features.assignmentMembership:true}") var assignmentMembership = defaults.assignmentMembership
	@Value("${features.extensions:true}") var extensions = defaults.extensions
	@Value("${features.feedbackTemplates:true}") var feedbackTemplates = defaults.feedbackTemplates
	@Value("${features.markingWorkflows:true}") var markingWorkflows = defaults.markingWorkflows
	@Value("${features.markerFeedback:true}") var markerFeedback = defaults.markerFeedback
	@Value("${features.profiles:true}") var profiles = defaults.profiles
	@Value("${features.assignmentProgressTable:true}") var assignmentProgressTable = defaults.assignmentProgressTable
	@Value("${features.assignmentProgressTableByDefault:false}") var assignmentProgressTableByDefault = defaults.assignmentProgressTableByDefault
	@Value("${features.summativeFilter:true}") var summativeFilter = defaults.summativeFilter
	
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
	var feedbackTemplates = true
	var markingWorkflows = true
	var markerFeedback = true
	var profiles = true
	var assignmentProgressTable = true
	var assignmentProgressTableByDefault = false
	var summativeFilter = true
}

class FeatureFlagListener extends QueueListener with InitializingBean with Logging {
	
		var queue = Wire.named[Queue]("settingsSyncTopic")
		var features = Wire.auto[Features]
		var context = Wire.property("${module.context}")
		
		override def isListeningToQueue = true
		override def onReceive(item: Any) {
				logger.info("Synchronising item " + item + " for " + context)
				item match {
						case copy: FeaturesMessage => features.update(copy)
						case _ => // Should never happen
				}
		}
		
		override def afterPropertiesSet() {
				logger.info("Registering listener for " + classOf[FeaturesMessage].getAnnotation(classOf[ItemType]).value + " on " + context)
				queue.addListener(classOf[FeaturesMessage].getAnnotation(classOf[ItemType]).value, this)
		}
	
}

object Features {
	def empty = new FeaturesImpl
}
