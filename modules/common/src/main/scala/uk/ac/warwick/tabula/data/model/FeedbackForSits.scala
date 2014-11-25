package uk.ac.warwick.tabula.data.model

import javax.persistence.FetchType._
import javax.persistence._

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser

@Entity
class FeedbackForSits extends GeneratedId {

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "feedback_id")
	var feedback: Feedback = _

	var status: FeedbackForSitsStatus = _
	var dateOfUpload: DateTime = _
	var actualMarkLastUploaded: Integer = _
	var actualGradeLastUploaded: String = _
	var firstCreatedOn: DateTime = _
	var lastInitialisedOn: DateTime = _
	var createdBy: CurrentUser = _


	def init(feedback: Feedback, submitter: CurrentUser): Unit = {
		this.feedback = feedback
		this.createdBy = submitter
		this.lastInitialisedOn = DateTime.now()
		this.status = FeedbackForSitsStatus.UploadNotAttempted
	}

}
