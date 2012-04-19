package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.TestBase
import org.junit.Test
import uk.ac.warwick.courses.RequestInfo
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.validation.BindException
import org.joda.time.DateTime
import uk.ac.warwick.courses.data.model.Submission

class SubmitAssignmentCommandTest extends TestBase {
	@Test def multipleSubmissions = withUser(code="cusebr", universityId="0678022") {
		val assignment = newActiveAssignment
		val user = RequestInfo.fromThread.get.user
		val cmd = new SubmitAssignmentCommand(assignment, user)
		
		var errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be (false)
		
		val submission = new Submission()
		submission.assignment = assignment
		submission.universityId = "0678022"
		assignment.submissions.add(submission)
		
		// Can't submit twice, silly
		errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be (true)
		
		// But guys, guys... what if...
		assignment.allowResubmission = true
		errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be (false)
	}
	
	def newActiveAssignment = {
		val assignment = new Assignment
		assignment.openDate = new DateTime().minusWeeks(1)
		assignment.closeDate = new DateTime().plusWeeks(1)
		assignment.collectSubmissions = true
		assignment.active = true
		assignment
	}
}