package uk.ac.warwick.tabula.coursework.commands.feedback
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.AuditEventIndexService
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.services.UserLookupService
import org.joda.time.DateTime
import uk.ac.warwick.userlookup.User


class ListFeedbackCommand(val module: Module, val assignment: Assignment) extends Command[Seq[(User, DateTime)]] with ReadOnly with Unaudited {
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Feedback.Read, assignment)
	
	var auditIndexService = Wire[AuditEventIndexService]
	var userLookup = Wire[UserLookupService]
	
	override def applyInternal() =
		auditIndexService.feedbackDownloads(assignment).map(x =>{(userLookup.getUserByUserId(x._1), x._2)})

	override def describe(d: Description) =	d.assignment(assignment)
}

case class FeedbackListItem(feedback: Feedback, downloaded: Boolean)