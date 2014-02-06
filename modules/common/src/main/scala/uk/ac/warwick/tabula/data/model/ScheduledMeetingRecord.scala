package uk.ac.warwick.tabula.data.model

import javax.persistence._

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("scheduled")
class ScheduledMeetingRecord extends AbstractMeetingRecord {

	def this(creator: Member, relationship: StudentRelationship[_]) {
		this()
		this.creator = creator
		this.relationship = relationship
	}

	var missed: Boolean = false

	@Column(name="missed_reason")
	var missedReason: String = _

	def pendingActionBy(member: Member): Boolean = member == creator && meetingDate.isBeforeNow && !missed

}