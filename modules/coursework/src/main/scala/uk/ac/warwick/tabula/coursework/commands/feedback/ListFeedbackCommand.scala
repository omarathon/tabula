package uk.ac.warwick.tabula.coursework.commands.feedback
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Feedback}
import uk.ac.warwick.tabula.services.AuditEventIndexService
import uk.ac.warwick.tabula.services.UserLookupService
import org.joda.time.DateTime
import uk.ac.warwick.userlookup.User


class ListFeedbackCommand(val module: Module, val assignment: Assignment) extends Command[ListFeedbackResult] with ReadOnly with Unaudited {
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Feedback.Read, assignment)
	
	var auditIndexService = Wire[AuditEventIndexService]
	var userLookup = Wire[UserLookupService]
	
	override def applyInternal() = {
	 ListFeedbackResult(
			auditIndexService.feedbackDownloads(assignment).map(userIdToUser),
		  auditIndexService.latestOnlineFeedbackViews(assignment).map(userIdToUser),
		  auditIndexService.latestOnlineFeedbackAdded(assignment).map(warwickIdToUser),
			auditIndexService.latestGenericFeedbackAdded(assignment)
	 )
	}

	def userIdToUser(tuple: (String, DateTime)) = tuple match {
		case (id, date) => (userLookup.getUserByUserId(id), date)
	}

	def warwickIdToUser(tuple: (String, DateTime)) = tuple match {
		case (id, date) => (userLookup.getUserByWarwickUniId(id), date)
	}

	override def describe(d: Description) =	d.assignment(assignment)
}

case class ListFeedbackResult(
	downloads: Seq[(User, DateTime)],
	latestOnlineViews: Seq[(User, DateTime)],
	latestOnlineAdded: Seq[(User, DateTime)],
  latestGenericFeedback: Option[DateTime]
)

case class FeedbackListItem(feedback: Feedback, downloaded: Boolean, onlineViewed: Boolean)