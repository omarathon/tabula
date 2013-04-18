package uk.ac.warwick.tabula


import org.codehaus.jackson.annotate.JsonAutoDetect
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.factory.InitializingBean

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.util.queue.QueueListener
import uk.ac.warwick.util.queue.conversion.ItemType
import uk.ac.warwick.tabula.JavaImports._


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
	
	var emailStudents = Wire[JBoolean]("${features.emailStudents:false}")
	var collectRatings = Wire[JBoolean]("${features.collectRatings:true}")
	var submissions = Wire[JBoolean]("${features.submissions:true}")
	var privacyStatement = Wire[JBoolean]("${features.privacyStatement:true}")
	var collectMarks = Wire[JBoolean]("${features.collectMarks:true}")
	var turnitin = Wire[JBoolean]("${features.turnitin:true}")
	var assignmentMembership = Wire[JBoolean]("${features.assignmentMembership:true}")
	var extensions = Wire[JBoolean]("${features.extensions:true}")
	var combinedForm = Wire[JBoolean]("${features.combinedForm:true}")
	var feedbackTemplates = Wire[JBoolean]("${features.feedbackTemplates:true}")
	var markingWorkflows = Wire[JBoolean]("${features.markingWorkflows:true}")
	var markerFeedback = Wire[JBoolean]("${features.markerFeedback:true}")
	var profiles = Wire[JBoolean]("${features.profiles:true}")
	var assignmentProgressTable = Wire[JBoolean]("${features.assignmentProgressTable:false}")
	
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
	
	var emailStudents: Boolean = _
	var collectRatings: Boolean = _
	var submissions: Boolean = _
	var privacyStatement: Boolean = _
	var collectMarks: Boolean = _
	var turnitin: Boolean = _
	var assignmentMembership: Boolean = _
	var extensions: Boolean = _
	var combinedForm: Boolean = _
	var feedbackTemplates: Boolean = _
	var markingWorkflows: Boolean = _
	var markerFeedback: Boolean = _
	var profiles: Boolean = _
	var assignmentProgressTable: Boolean = _
}

class FeatureFlagListener extends QueueListener with InitializingBean with Logging {
	
		var queue = Wire[Queue]("settingsSyncTopic")
		var features = Wire[Features]
		var context = Wire[String]("${module.context}")
		
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
