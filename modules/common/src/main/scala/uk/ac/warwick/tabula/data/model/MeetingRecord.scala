package uk.ac.warwick.tabula.data.model

import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.ToString
import org.joda.time.DateTime
import org.hibernate.annotations.Type
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.system.permissions.RestrictionProvider
import uk.ac.warwick.tabula.data.model.MeetingApprovalState._
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import scala.Some

object MeetingRecord {
	val DefaultMeetingTimeOfDay = 12
	val MeetingTooOldThresholdYears = 5
	val MaxTitleLength = 140
}

@Entity
class MeetingRecord extends GeneratedId with PermissionsTarget with ToString with CanBeDeleted with FormattedHtml {

	@Column(name="creation_date")
	@Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var creationDate: DateTime = DateTime.now

	@Column(name="last_updated_date")
	@Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var lastUpdatedDate: DateTime = creationDate

	@ManyToOne
	@JoinColumn(name = "relationship_id")
	var relationship: StudentRelationship = _

	@Column(name="meeting_date")
	@Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var meetingDate: DateTime = _

	@Column(name="meeting_format")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.MeetingFormatUserType")
	var format: MeetingFormat = _

	@ManyToOne
	@JoinColumn(name="creator_id")
	var creator: Member = _

  def readPermissions():Seq[Permission]={
    Seq(relationship.relationshipType match {
      case RelationshipType.PersonalTutor =>Permissions.Profiles.PersonalTutor.MeetingRecord.ReadDetails
      case RelationshipType.Supervisor => Permissions.Profiles.Supervisor.MeetingRecord.ReadDetails
    })
  }

	@OneToMany(mappedBy="meetingRecord", fetch=FetchType.LAZY, cascade=Array(ALL))
  @RestrictionProvider("readPermissions")
	var attachments: JList[FileAttachment] = JArrayList()

  @RestrictionProvider("readPermissions")
	var title: String = _

  @RestrictionProvider("readPermissions")
	var description: String = _

	def escapedDescription:String = formattedHtml(description)

	def this(creator: Member, relationship: StudentRelationship) {
		this()
		this.creator = creator
		this.relationship = relationship
	}

	// Workflow definitions

	@OneToMany(mappedBy="meetingRecord", fetch=FetchType.LAZY, cascade=Array(ALL))
	var approvals: JList[MeetingRecordApproval] = JArrayList()

	// true if the specified user needs to perform a workflow action on this meeting record
	def pendingActionBy(member: Member): Boolean = pendingApprovalBy(member) || pendingRevisionBy(member)

	// if there are no approvals with a state of approved return true - otherwise, all approvals need to be true
	def isApproved = !approvals.asScala.exists(approval => !(approval.state == Approved))

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

	def permissionsParents = relationship.studentMember.toStream

	def toStringProps = Seq(
		"creator" -> creator,
		"creationDate" -> creationDate,
		"relationship" -> relationship)

}

sealed abstract class MeetingFormat(val code: String, val description: String) {
	override def toString = description
}

object MeetingFormat {
	case object FaceToFace extends MeetingFormat("f2f", "Face to face meeting")
	case object VideoConference extends MeetingFormat("video", "Video conference")
	case object PhoneCall extends MeetingFormat("phone", "Telephone call")
	case object Email extends MeetingFormat("email", "Email conversation")

	// lame manual collection. Keep in sync with the case objects above
	val members = Set(FaceToFace, VideoConference, PhoneCall, Email)

	def fromCode(code: String) =
		if (code == null) null
		else members.find{_.code == code} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}

	def fromDescription(description: String) =
		if (description == null) null
		else members.find{_.description == description} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}
}

class MeetingFormatUserType extends AbstractBasicUserType[MeetingFormat, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = MeetingFormat.fromCode(string)
	override def convertToValue(format: MeetingFormat) = format.code
}
