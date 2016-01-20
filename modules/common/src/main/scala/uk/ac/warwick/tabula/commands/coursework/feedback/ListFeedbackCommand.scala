package uk.ac.warwick.tabula.commands.coursework.feedback

import java.util.concurrent.ScheduledExecutorService

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.Futures
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{FeedbackForSits, Assignment, Module, Feedback}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.services.AuditEventIndexService
import uk.ac.warwick.tabula.services.UserLookupService
import org.joda.time.DateTime
import uk.ac.warwick.userlookup.User

import scala.concurrent.Await
import scala.concurrent.duration._

class ListFeedbackCommand(val module: Module, val assignment: Assignment)
	extends Command[ListFeedbackResult] with ReadOnly with Unaudited {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.AssignmentFeedback.Read, assignment)

	var auditIndexService = Wire[AuditEventIndexService]
	var userLookup = Wire[UserLookupService]
	implicit val taskSchedulerService = Wire[ScheduledExecutorService]

	override def applyInternal() = {
		// The time to wait for a query to complete
		val timeout = 15.seconds

		// Wrap each future in Future.optionalTimeout, which will return None if it times out early
		val downloads = Futures.optionalTimeout(auditIndexService.feedbackDownloads(assignment).map(_.map(userIdToUser)), timeout)
		val latestOnlineViews = Futures.optionalTimeout(auditIndexService.latestOnlineFeedbackViews(assignment).map(_.map(userIdToUser)), timeout)
		val latestOnlineAdded = Futures.optionalTimeout(auditIndexService.latestOnlineFeedbackAdded(assignment).map(_.map(warwickIdToUser)), timeout)
		val latestGenericFeedback = Futures.optionalTimeout(auditIndexService.latestGenericFeedbackAdded(assignment), timeout)

		val result = for {
			downloads <- downloads
			latestOnlineViews <- latestOnlineViews
			latestOnlineAdded <- latestOnlineAdded
			latestGenericFeedback <- latestGenericFeedback
		} yield ListFeedbackResult(
			downloads.getOrElse(Nil),
			latestOnlineViews.getOrElse(Nil),
			latestOnlineAdded.getOrElse(Nil),
			latestGenericFeedback.getOrElse(None)
		)

		// We arbitrarily wait a longer time for the result, safe in the knowledge that if they don't return in a reasonable
		// time then we've messed up.
		Await.result(result, timeout * 2)
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

case class FeedbackListItem(feedback: Feedback, downloaded: Boolean, onlineViewed: Boolean, feedbackForSits: FeedbackForSits)