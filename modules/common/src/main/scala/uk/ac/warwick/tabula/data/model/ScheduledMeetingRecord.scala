package uk.ac.warwick.tabula.data.model

import javax.persistence._
import uk.ac.warwick.tabula.timetables.{TimetableEvent, EventOccurrence}

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

	def toEventOccurrence(context: TimetableEvent.Context): Option[EventOccurrence] = asEventOccurrence(context)

	// false when the meeting has been scheduled by a third party
	def creatorInRelationship = {
		def isStudent = creator.universityId == relationship.studentId
		def isAgent = relationship.agentMember.exists(_ == creator)
		isStudent || isAgent
	}
}