package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.joda.time.DateTime
import org.hibernate.annotations.Type

@Entity
class FeedbackForSits extends GeneratedId {

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "feedback_id")
	var feedback: Feedback = _


	@Column(name="status")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.FeedbackForSitsStatusUserType")
	var status: FeedbackForSitsStatus = _

	var dateOfUpload: DateTime = _
	var actualMarkLastUploaded: Integer = _
	var actualGradeLastUploaded: String = _
	var firstCreatedOn: DateTime = _
	var lastInitialisedOn: DateTime = _
	var initialiserId: String =_


	def init(feedback: Feedback, creatorId: String): Unit = {
		this.feedback = feedback
		this.initialiserId = creatorId
		this.lastInitialisedOn = DateTime.now()
		this.status = FeedbackForSitsStatus.UploadNotAttempted
	}

}
