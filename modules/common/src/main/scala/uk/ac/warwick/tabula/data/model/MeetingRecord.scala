package uk.ac.warwick.tabula.data.model

import javax.persistence._
import javax.persistence.CascadeType._
import org.hibernate.annotations.BatchSize
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.MeetingApprovalState._
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType, EventOccurrence}

object MeetingRecord {
	val DefaultMeetingTimeOfDay = 12 // Should be used for legacy meetings (where isRealTime is false)
	val MeetingTooOldThresholdYears = 5
	val MaxTitleLength = 140
}

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("standard")
class MeetingRecord extends AbstractMeetingRecord {

	def this(creator: Member, relationship: StudentRelationship) {
		this()
		this.creator = creator
		this.relationship = relationship
	}

	@Column(name="real_time")
	var isRealTime: Boolean = true

	def toEventOccurrence(context: TimetableEvent.Context): Option[EventOccurrence] = {
		if (isRealTime) {
			this.asEventOccurrence(context)
		}	else {
			None
		}
	}

	// Workflow definitions

	@OneToMany(mappedBy="meetingRecord", fetch=FetchType.LAZY, cascade=Array(ALL))
	@BatchSize(size=200)
	var approvals: JList[MeetingRecordApproval] = JArrayList()

	// true if the specified user needs to perform a workflow action on this meeting record
	def pendingActionBy(member: Member): Boolean = pendingApprovalBy(member) || pendingRevisionBy(member)

	// if there are no approvals with a state of approved return true - otherwise, all approvals need to be true
	def isApproved = !approvals.asScala.exists(approval => !(approval.state == Approved))

	// for attendance purposes the meeting is approved if it was created by the agent, or is otherwise approved
	def isAttendanceApproved = (creator != null && relationship != null && creator.universityId == relationship.agent) || isApproved

	def isPendingApproval = approvals.asScala.exists(approval => approval.state == Pending)
	def pendingApprovals = approvals.asScala.filter(_.state == Pending)
	def pendingApprovalBy(member: Member): Boolean = pendingApprovals.exists(_.approver == member)
	def pendingApprovers:List[Member] = pendingApprovals.map(_.approver).toList

	def isRejected =  approvals.asScala.exists(approval => approval.state == Rejected)
	def rejectedApprovals = approvals.asScala.filter(_.state == Rejected)
	def rejectedBy(member: Member): Boolean = rejectedApprovals.exists(_.approver == member)
	// people who have had a draft version rejected
	def pendingRevisionBy(member: Member) = isRejected && member == creator

	// End of workflow definitions
}