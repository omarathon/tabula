package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.model._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.AssignmentService
import org.springframework.transaction.annotation.Transactional
import org.joda.time.DateTime
import uk.ac.warwick.courses.CurrentUser

class SubmitAssignmentCommand(val assignment:Assignment, val user:CurrentUser) extends Command[Submission] {

  @Autowired var service:AssignmentService =_

  @Transactional
  override def apply = {
	val submission = new Submission
	submission.assignment = assignment
	submission.submitted = true
	submission.submittedDate = new DateTime
	submission.userId = user.apparentUser.getUserId
	submission.universityId = user.apparentUser.getWarwickId
	// FIXME add submission values! especially attachments!
	service.saveSubmission(submission)
	submission
  }

  override def describe(d: Description) = d.properties(
    
  )
  

}