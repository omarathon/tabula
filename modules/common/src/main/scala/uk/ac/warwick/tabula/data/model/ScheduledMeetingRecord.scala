package uk.ac.warwick.tabula.data.model

import javax.persistence._
import uk.ac.warwick.tabula.timetables.EventOccurrence

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("scheduled")
class ScheduledMeetingRecord extends AbstractMeetingRecord {

	def this(creator: Member, relationship: StudentRelationship) {
		this()
		this.creator = creator
		this.relationship = relationship
	}

	var missed: Boolean = false

	@Column(name="missed_reason")
	var missedReason: String = _

	def isPendingAction = meetingDate.isBeforeNow && !missed
	def pendingActionBy(member: Member): Boolean = member == creator && isPendingAction

	def toEventOccurrence: Option[EventOccurrence] = asEventOccurrence

}