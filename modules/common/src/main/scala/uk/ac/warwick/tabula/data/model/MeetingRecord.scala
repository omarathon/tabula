package uk.ac.warwick.tabula.data.model

import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import org.joda.time.DateTime
import org.hibernate.annotations.Type
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.JavaImports._

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

	@ManyToOne
	@JoinColumn(name="creator_id")
	var creator: Member = _

	@OneToMany(mappedBy = "meetingrecord", fetch = FetchType.LAZY)
	var attachments: JList[FileAttachment] = JArrayList()

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