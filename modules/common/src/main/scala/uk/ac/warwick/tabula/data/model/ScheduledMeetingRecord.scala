package uk.ac.warwick.tabula.data.model

import javax.persistence._

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("scheduled")
class ScheduledMeetingRecord extends AbstractMeetingRecord {

	def this(creator: Member, relationship: StudentRelationship) {
		this()
		this.creator = creator
		this.relationship = relationship
	}

	def pendingActionBy(member: Member): Boolean = member == creator && meetingDate.isBeforeNow

}