package uk.ac.warwick.tabula.data.model

import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import org.joda.time.DateTime
import org.hibernate.annotations.Type

@Entity
class MeetingRecordApproval extends GeneratedId  {
	@ManyToOne
	@JoinColumn(name = "meetingrecord_id")
	var meetingRecord: MeetingRecord = _

	@ManyToOne
	@JoinColumn(name="approver_id")
	var approver: Member = _
	
	var approved: Boolean = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var lastUpdatedDate = DateTime.now

}
