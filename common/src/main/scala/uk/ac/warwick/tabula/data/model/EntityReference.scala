package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.{Any, AnyMetaDef, MetaValue}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpointTotal, AttendanceMonitoringScheme, MonitoringPointReport}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.groups._

/**
 * Stores a reference to an entity that is being pointed at in a
 * Notification.
 */
@Entity
class EntityReference[A >: Null <: ToEntityReference] extends GeneratedId {
	// Maps to Notification.items
	@ManyToOne
	@JoinColumn(name = "notification_id")
	var notification: Notification[_, _] = _

	@Column(name = "entity_type", insertable = false, updatable = false)
	var entityType: String = _

	@Any(metaColumn = new Column(name = "entity_type"), fetch = FetchType.EAGER, optional = false)
	@AnyMetaDef(idType = "string", metaType = "string",
		metaValues = Array(
			new MetaValue(targetEntity = classOf[Assignment], value = "assignment"),
			new MetaValue(targetEntity = classOf[Submission], value = "submission"),
			new MetaValue(targetEntity = classOf[AssignmentFeedback], value = "feedback"),
			new MetaValue(targetEntity = classOf[ExamFeedback], value = "examFeedback"),
			new MetaValue(targetEntity = classOf[MarkerFeedback], value = "markerFeedback"),
			new MetaValue(targetEntity = classOf[Module], value = "module"),
			new MetaValue(targetEntity = classOf[Extension], value = "extension"),
			new MetaValue(targetEntity = classOf[StudentRelationship], value = "studentRelationship"),
			new MetaValue(targetEntity = classOf[MeetingRecord], value = "meetingRecord"),
			new MetaValue(targetEntity = classOf[ScheduledMeetingRecord], value = "scheduledMeetingRecord"),
			new MetaValue(targetEntity = classOf[MeetingRecordApproval], value = "meetingRecordApprovel"),
			new MetaValue(targetEntity = classOf[SmallGroup], value = "smallGroup"),
			new MetaValue(targetEntity = classOf[SmallGroupSet], value = "smallGroupSet"),
			new MetaValue(targetEntity = classOf[SmallGroupEvent], value = "smallGroupEvent"),
			new MetaValue(targetEntity = classOf[SmallGroupEventOccurrence], value = "smallGroupEventOccurrence"),
			new MetaValue(targetEntity = classOf[DepartmentSmallGroupSet], value = "departmentSmallGroupSet"),
			new MetaValue(targetEntity = classOf[DepartmentSmallGroup], value = "departmentSmallGroup"),
			new MetaValue(targetEntity = classOf[OriginalityReport], value = "originalityReport"),
			new MetaValue(targetEntity = classOf[Department], value = "department"),
			new MetaValue(targetEntity = classOf[Exam], value = "exam"),
			new MetaValue(targetEntity = classOf[AttendanceMonitoringScheme], value = "attendanceMonitoringScheme"),
			new MetaValue(targetEntity = classOf[AttendanceMonitoringCheckpointTotal], value = "attendanceMonitoringCheckpointTotal"),
			new MetaValue(targetEntity = classOf[MonitoringPointReport], value = "MonitoringPointReport"),
		)
	)
	@JoinColumn(name="entity_id")
	var entity: A = _

	def put(e: A): this.type = {
		this.entity = e
		this
	}

	type Entity = A
}

object EntityReference {
	def supports(cls: Class[_]): Boolean = cls match {
		case t if isSubtype(t, classOf[Assignment]) => true
		case t if isSubtype(t, classOf[Submission]) => true
		case t if isSubtype(t, classOf[AssignmentFeedback]) => true
		case t if isSubtype(t, classOf[ExamFeedback]) => true
		case t if isSubtype(t, classOf[MarkerFeedback]) => true
		case t if isSubtype(t, classOf[Module]) => true
		case t if isSubtype(t, classOf[Extension]) => true
		case t if isSubtype(t, classOf[StudentRelationship]) => true
		case t if isSubtype(t, classOf[MeetingRecord]) => true
		case t if isSubtype(t, classOf[ScheduledMeetingRecord]) => true
		case t if isSubtype(t, classOf[MeetingRecordApproval]) => true
		case t if isSubtype(t, classOf[SmallGroup]) => true
		case t if isSubtype(t, classOf[SmallGroupSet]) => true
		case t if isSubtype(t, classOf[SmallGroupEvent]) => true
		case t if isSubtype(t, classOf[SmallGroupEventOccurrence]) => true
		case t if isSubtype(t, classOf[DepartmentSmallGroupSet]) => true
		case t if isSubtype(t, classOf[DepartmentSmallGroup]) => true
		case t if isSubtype(t, classOf[OriginalityReport]) => true
		case t if isSubtype(t, classOf[Department]) => true
		case t if isSubtype(t, classOf[Exam]) => true
		case t if isSubtype(t, classOf[AttendanceMonitoringScheme]) => true
		case t if isSubtype(t, classOf[AttendanceMonitoringCheckpointTotal]) => true
		case t if isSubtype(t, classOf[MonitoringPointReport]) => true
		case _ => false
	}

	private def isSubtype[A, B](self: Class[A], other: Class[B]) = other.isAssignableFrom(self)

	def apply[A >: Null <: ToEntityReference](entity: A): EntityReference[A] =
		if (supports(entity.getClass)) new EntityReference[A].put(entity)
		else throw new IllegalArgumentException(s"Unsupported EntityReference for ${entity.getClass}")
}
