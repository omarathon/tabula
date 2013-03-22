package uk.ac.warwick.tabula.data.model

import scala.reflect.BeanProperty
import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import org.joda.time.DateTime
import org.hibernate.annotations.Type
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.DateFormats

@Entity
class MeetingRecord extends GeneratedId with ToString {
	@ManyToOne
	@JoinColumn(name = "relationship_id")
	@BeanProperty var relationship: StudentRelationship = _
	
	@Column(name="creation_date")
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var creationDate: DateTime = _
	
	@Column(name="last_updated_date")
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var lastUpdatedDate: DateTime = _
	
	@Column(name="meeting_date")
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var meetingDate: DateTime = _
	
	@ManyToOne
	@JoinColumn(name="creator_id")
	@BeanProperty var creator: Member = _
	
	@BeanProperty var title: String = _
	@BeanProperty var description: String = _
	
	def this(creator: Member, relationship: StudentRelationship) {
		this()
		this.creator = creator
		this.relationship = relationship
		
		this.creationDate = DateTime.now
		this.lastUpdatedDate = this.creationDate
	}
	
	def isApproved = false
	
	def toStringProps = Seq(
		"creator" -> creator,
		"creationDate" -> creationDate,
		"relationship" -> relationship)

}