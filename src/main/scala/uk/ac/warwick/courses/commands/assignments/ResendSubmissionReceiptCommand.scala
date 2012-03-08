package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.util.mail.WarwickMailSender
import javax.annotation.Resource
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.data.model.Submission

class SendSubmissionReceiptCommand(
		@BeanProperty var submission:Submission = null,
		@BeanProperty var user:CurrentUser = null
		) extends Command[Unit] {
	
	@BeanProperty var assignment:Assignment = Option(submission).map{_.assignment}.orNull
	@BeanProperty var module:Module = Option(assignment).map{_.module}.orNull
	
	@Resource(name="studentMailSender") var studentMailSender:WarwickMailSender =_
	
	def apply {
		
	}
	
	override def describe(d:Description) {
		d.assignment(assignment)
	}
	
}