package uk.ac.warwick.tabula.data.model

import javax.persistence._
import uk.ac.warwick.tabula.permissions.{Permissions, Permission}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupSet, SmallGroup}
import org.hibernate.annotations.ForeignKey

/**
 * Stores a reference to an entity that is being pointed at in a
 * Notification. There is a subclass of this for every different
 * entity that we would need to point to. Hibernate's subclass magic
 * will work out which one to make and thus which table to get the
 * object from.
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "entity_type")
abstract class EntityReference[A >: Null <: AnyRef] extends GeneratedId {
	// Maps to Notification.items
	@ManyToOne
	@JoinColumn(name = "notification_id")
	var notification: Notification[_, _] = null

	var entity: A

	def put(e: A): this.type = {
		this.entity = e
		this
	}

	type Entity = A
}


@Entity @DiscriminatorValue(value="assignment")
class AssignmentEntityReference extends EntityReference[Assignment] {
	@ManyToOne(fetch = FetchType.LAZY)
	var entity: Entity = null
}

@Entity @DiscriminatorValue(value="submission")
class SubmissionEntityReference extends EntityReference[Submission] {
	@ManyToOne(fetch = FetchType.LAZY)
	var entity: Entity = null
}

@Entity @DiscriminatorValue(value="feedback")
class FeedbackEntityReference extends EntityReference[Feedback] {
	@ManyToOne(fetch = FetchType.LAZY)
	var entity: Entity = null
}

@Entity @DiscriminatorValue(value="markerFeedback")
class MarkerFeedbackEntityReference extends EntityReference[MarkerFeedback] {
	@ManyToOne(fetch = FetchType.LAZY)
	var entity: Entity = null
}

@Entity @DiscriminatorValue(value="module")
class ModuleEntityReference extends EntityReference[Module] {
	@ManyToOne(fetch = FetchType.LAZY)
	var entity: Entity = null
}

@Entity @DiscriminatorValue(value="extension")
class ExtensionEntityReference extends EntityReference[Extension] {
	@ManyToOne(fetch = FetchType.LAZY)
	var entity: Entity = null
}

@Entity @DiscriminatorValue(value="studentRelationship")
class StudentRelationshipEntityReference extends EntityReference[StudentRelationship] {
	@ManyToOne(fetch = FetchType.LAZY)
	var entity: Entity = null
}

@Entity @DiscriminatorValue(value="meetingRecord")
class MeetingRecordEntityReference extends EntityReference[AbstractMeetingRecord] {
	@ManyToOne(fetch = FetchType.LAZY)
	var entity: Entity = null
}

@Entity @DiscriminatorValue(value="meetingRecordApprovel")
class MeetingRecordApprovalEntityReference extends EntityReference[MeetingRecordApproval] {
	@ManyToOne(fetch = FetchType.LAZY)
	var entity: Entity = null
}

@Entity @DiscriminatorValue(value="smallGroup")
class SmallGroupEntityReference extends EntityReference[SmallGroup] {
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	var entity: Entity = null
}

@Entity @DiscriminatorValue(value="smallGroupSet")
class SmallGroupSetEntityReference extends EntityReference[SmallGroupSet] {
	@ManyToOne(fetch = FetchType.LAZY)
	var entity: Entity = null
}

@Entity @DiscriminatorValue(value="smallGroupEvent")
class SmallGroupEventEntityReference extends EntityReference[SmallGroupEvent] {
	@ManyToOne(fetch = FetchType.LAZY)
	var entity: Entity = null
}

@Entity @DiscriminatorValue(value="originalityReport")
class OriginalityReportEntityReference extends EntityReference[OriginalityReport] {
	@ManyToOne(fetch = FetchType.LAZY)
	var entity: Entity = null
}