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
import scala.beans.BeanProperty


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
	@Value("${features.assignmentProgressTableByDefault:true}") var assignmentProgressTableByDefault = defaults.assignmentProgressTableByDefault
	@Value("${features.summativeFilter:true}") var summativeFilter = defaults.summativeFilter
	@Value("${features.meetingRecordApproval:true}") var meetingRecordApproval = defaults.meetingRecordApproval
	@Value("${features.smallGroupTeaching:true}") var smallGroupTeaching = defaults.smallGroupTeaching
	@Value("${features.smallGroupTeaching.studentSignUp:false}") var smallGroupTeachingStudentSignUp = defaults.smallGroupTeachingStudentSignUp
	@Value("${features.smallGroupTeaching.randomAllocation:false}") var smallGroupTeachingRandomAllocation = defaults.smallGroupTeachingRandomAllocation
	@Value("${features.smallGroupTeaching.selfGroupSwitching:false}") var smallGroupTeachingSelfGroupSwitching = defaults.smallGroupTeachingSelfGroupSwitching
	@Value("${features.smallGroupTeaching.tutorView:true}") var smallGroupTeachingTutorView = defaults.smallGroupTeachingTutorView
	@Value("${features.attendanceMonitoring:true}") var attendanceMonitoring = defaults.attendanceMonitoring
	@Value("${features.smallGroupAllocationFiltering:false}") var smallGroupAllocationFiltering = defaults.smallGroupAllocationFiltering
	@Value("${features.personalTutorAssignment:false}") var personalTutorAssignment = defaults.personalTutorAssignment
	@Value("${features.personalTutorAssignmentFiltering:false}") var personalTutorAssignmentFiltering = defaults.personalTutorAssignmentFiltering
	@Value("${features.arbitraryRelationships:false}") var arbitraryRelationships = defaults.arbitraryRelationships

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

	// BeanProperty current required as Utils JsonMessageConverter uses Jackson
	// without the Scala module.
	@BeanProperty var emailStudents = false
	@BeanProperty var collectRatings = true
	@BeanProperty var submissions = true
	@BeanProperty var privacyStatement = true
	@BeanProperty var collectMarks = true
	@BeanProperty var turnitin = true
	@BeanProperty var assignmentMembership = true
	@BeanProperty var extensions = true
	@BeanProperty var feedbackTemplates = true
	@BeanProperty var markingWorkflows = true
	@BeanProperty var markerFeedback = true
	@BeanProperty var profiles = true
	@BeanProperty var assignmentProgressTable = true
	@BeanProperty var assignmentProgressTableByDefault = true
	@BeanProperty var summativeFilter = true
	@BeanProperty var meetingRecordApproval = true
	@BeanProperty var smallGroupTeaching = true
	@BeanProperty var smallGroupTeachingStudentSignUp = false
	@BeanProperty var smallGroupTeachingRandomAllocation = false
	@BeanProperty var smallGroupTeachingSelfGroupSwitching = false
	@BeanProperty var smallGroupTeachingTutorView = false
	@BeanProperty var attendanceMonitoring = true
	@BeanProperty var smallGroupAllocationFiltering = false
	@BeanProperty var personalTutorAssignment = false
	@BeanProperty var personalTutorAssignmentFiltering = false
	@BeanProperty var arbitraryRelationships = false
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
