package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.{Assignment, Module, MeetingFormat, StudentRelationshipType, HasSettings, GeneratedId}
import javax.persistence._
import javax.validation.constraints.NotNull
import org.joda.time.DateTime
import scala.Array
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.annotations.{Type, BatchSize}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{AssignmentService, ModuleAndDepartmentService, RelationshipService}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.PostLoadBehaviour

@Entity
class MonitoringPoint extends CommonMonitoringPointProperties with MonitoringPointSettings {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "point_set_id")
	var pointSet: MonitoringPointSet = _

	@OneToMany(mappedBy = "point", cascade=Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var checkpoints: JList[MonitoringCheckpoint] = JArrayList()

	def isLate(currentAcademicWeek: Int): Boolean = {
		currentAcademicWeek > requiredFromWeek
	}

	var sentToAcademicOffice: Boolean = false
}

trait CommonMonitoringPointProperties extends GeneratedId {
	@NotNull
	var name: String = _

	var createdDate: DateTime = _

	var updatedDate: DateTime = _

	@NotNull
	var validFromWeek: Int = _

	@NotNull
	var requiredFromWeek: Int = _

	@Column(name="point_type")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.attendance.MonitoringPointTypeUserType")
	var pointType: MonitoringPointType = _

	def toPoint = {
		val point = new MonitoringPoint
		point.createdDate = new DateTime()
		point.updatedDate = new DateTime()
		point.name = name
		point.validFromWeek = validFromWeek
		point.requiredFromWeek = requiredFromWeek
		point.pointType = pointType
		point
	}

	def includesWeek(week: Int) = (validFromWeek to requiredFromWeek).contains(week)
}

trait MonitoringPointSettings extends HasSettings with PostLoadBehaviour {
	import MonitoringPoint._

	@transient
	var relationshipService = Wire[RelationshipService]

	@transient
	var moduleAndDepartmentService = Wire[ModuleAndDepartmentService]

	@transient
	var assignmentService = Wire[AssignmentService]

	override def postLoad() {
		ensureSettings
	}

	// Setting for MonitoringPointType.Meeting
	def meetingRelationships = getStringSeqSetting(Settings.MeetingRelationships, Seq()).map(relationshipService.getStudentRelationshipTypeById(_).getOrElse(null))
	def meetingRelationships_= (relationships: Seq[StudentRelationshipType]):Unit =
		settings += (Settings.MeetingRelationships -> relationships.map(_.id))
	// Ugh. This sucks. But Spring always wants to use the Seq version if they share a method name, and therefore won't bind
	def meetingRelationshipsSpring_= (relationships: JSet[StudentRelationshipType]):Unit = {
		meetingRelationships = relationships.asScala.toSeq
	}

	def meetingFormats = getStringSeqSetting(Settings.MeetingFormats, Seq()).map(MeetingFormat.fromDescription)
	def meetingFormats_= (formats: Seq[MeetingFormat]) =
		settings += (Settings.MeetingFormats -> formats.map(_.description))
	// See above
	def meetingFormatsSpring_= (formats: JSet[MeetingFormat]) =
		meetingFormats = formats.asScala.toSeq

	def meetingQuantity = getIntSetting(Settings.MeetingQuantity, 1)
	def meetingQuantity_= (quantity: Int) = settings += (Settings.MeetingQuantity -> quantity)

	// Setting for MonitoringPointType.SmallGroup

	def smallGroupEventQuantity = getIntSetting(Settings.SmallGroupEventQuantity, 0)
	def smallGroupEventQuantity_= (quantity: Int): Unit = settings += (Settings.SmallGroupEventQuantity -> quantity)
	def smallGroupEventQuantity_= (quantity: JInteger): Unit = {
		smallGroupEventQuantity = quantity match {
			case q: JInteger => q.intValue
			case _ => 0
		}
	}

	def smallGroupEventModules = getStringSeqSetting(Settings.SmallGroupEventModules, Seq()).map(moduleAndDepartmentService.getModuleById(_).getOrElse(null))
	def smallGroupEventModules_= (modules: Seq[Module]) =
		settings += (Settings.SmallGroupEventModules -> modules.map(_.id))
	// See above
	def smallGroupEventModulesSpring_= (modules: JSet[Module]) =
		smallGroupEventModules = modules.asScala.toSeq

	// Setting for MonitoringPointType.AssignmentSubmission

	def assignmentSubmissionIsSpecificAssignments = getBooleanSetting(Settings.AssignmentSubmissionIsSpecificAssignments) getOrElse true
	def assignmentSubmissionIsSpecificAssignments_= (allow: Boolean) = settings += (Settings.AssignmentSubmissionIsSpecificAssignments -> allow)

	def assignmentSubmissionQuantity = getIntSetting(Settings.AssignmentSubmissionQuantity, 0)
	def assignmentSubmissionQuantity_= (quantity: Int): Unit = settings += (Settings.AssignmentSubmissionQuantity -> quantity)
	def assignmentSubmissionQuantity_= (quantity: JInteger): Unit = {
		assignmentSubmissionQuantity = quantity match {
			case q: JInteger => q.intValue
			case _ => 0
		}
	}

	def assignmentSubmissionModules = getStringSeqSetting(Settings.AssignmentSubmissionAssignments, Seq()).map(moduleAndDepartmentService.getModuleById(_).getOrElse(null))
	def assignmentSubmissionModules_= (modules: Seq[Module]) =
		settings += (Settings.AssignmentSubmissionModules -> modules.map(_.id))
	// See above
	def assignmentSubmissionModulesSpring_= (modules: JSet[Module]) =
		assignmentSubmissionModules = modules.asScala.toSeq

	def assignmentSubmissionAssignments = getStringSeqSetting(Settings.AssignmentSubmissionAssignments, Seq()).map(assignmentService.getAssignmentById(_).getOrElse(null))
	def assignmentSubmissionAssignments_= (assignments: Seq[Assignment]) =
		settings += (Settings.AssignmentSubmissionAssignments -> assignments.map(_.id))
	// See above
	def assignmentSubmissionAssignmentsSpring_= (assignments: JSet[Assignment]) =
		assignmentSubmissionAssignments = assignments.asScala.toSeq

	def assignmentSubmissionIsDisjunction = getBooleanSetting(Settings.AssignmentSubmissionIsDisjunction) getOrElse false
	def assignmentSubmissionIsDisjunction_= (allow: Boolean) = settings += (Settings.AssignmentSubmissionIsDisjunction -> allow)
}

object MonitoringPoint {

	object Settings {
		val MeetingRelationships = "meetingRelationships"
		val MeetingFormats = "meetingFormats"
		val MeetingQuantity = "meetingQuantity"

		val SmallGroupEventQuantity = "smallGroupEventQuantity"
		val SmallGroupEventModules = "smallGroupEventModules"

		val AssignmentSubmissionIsSpecificAssignments = "assignmentSubmissionIsSpecificAssignments"
		val AssignmentSubmissionQuantity = "assignmentSubmissionQuantity"
		val AssignmentSubmissionModules = "assignmentSubmissionModules"
		val AssignmentSubmissionAssignments = "assignmentSubmissionAssignments"
		val AssignmentSubmissionIsDisjunction = "assignmentSubmissionIsDisjunction"
	}
}
