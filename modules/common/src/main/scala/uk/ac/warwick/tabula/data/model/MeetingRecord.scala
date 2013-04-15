package uk.ac.warwick.tabula.data.model

import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import org.joda.time.DateTime
import org.hibernate.annotations.Type
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.DateFormats
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

@Entity
class MeetingRecord extends GeneratedId with ToString {
	@Column(name="creation_date")
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var creationDate: DateTime = DateTime.now
	
	@Column(name="last_updated_date")
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var lastUpdatedDate: DateTime = creationDate
	
	@ManyToOne
	@JoinColumn(name = "relationship_id")
	var relationship: StudentRelationship = _
	
	@Column(name="meeting_date")
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var meetingDate: DateTime = _
	
	@Column(name="meeting_format")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.MeetingFormatUserType")
	var format: MeetingFormat = _
	
	@ManyToOne
	@JoinColumn(name="creator_id")
	var creator: Member = _
	
	var title: String = _
	var description: String = _
	
	def this(creator: Member, relationship: StudentRelationship) {
		this()
		this.creator = creator
		this.relationship = relationship
	}
	
	def isApproved = false
	
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
