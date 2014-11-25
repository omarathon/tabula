package uk.ac.warwick.tabula.data.model

import javax.persistence.FetchType._
import javax.persistence._

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser

@Entity
class FeedbackForSits {

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "id")
	var feedback: Feedback = _

	var status: FeedbackForSitsStatus = _
	var dateOfUpload: DateTime = _
	var actualMarkLastUploaded: Integer = _
	var actualGradeLastUploaded: String = _
	var createdOn: DateTime = _
	var createdBy: CurrentUser = _

	def init(feedback: Feedback, submitter: CurrentUser): Unit = {
		this.feedback = feedback
		this.createdBy = submitter
		this.createdOn = DateTime.now()
		this.status = FeedbackForSitsStatus.UploadNotAttempted
	}

}
