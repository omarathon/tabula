package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model._
import forms.Extension
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Submission}
import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.services.{UserLookupService, AssignmentService, AuditEventIndexService}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.userlookup.User

class SubmissionAndFeedbackCommand(val module: Module, val assignment: Assignment) extends Command[Unit] with Unaudited with ReadOnly {
	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Submission.Read, assignment)

	var auditIndexService = Wire.auto[AuditEventIndexService]
	var assignmentService = Wire.auto[AssignmentService]
	var userLookup = Wire.auto[UserLookupService]

	@BeanProperty var students:Seq[Item] = _
	@BeanProperty var awaitingSubmissionExtended:Seq[(User, Extension)] = _
	@BeanProperty var awaitingSubmission:Seq[User] = _
	@BeanProperty var whoDownloaded: Seq[User] = _
	@BeanProperty var stillToDownload: Set[User] =_
	@BeanProperty var hasPublishedFeedback: Boolean =_
	@BeanProperty var hasOriginalityReport: Boolean =_
	@BeanProperty var mustReleaseForMarking: Boolean =_

	val enhancedSubmissionsCommand = new ListSubmissionsCommand(module, assignment)

	def applyInternal(){
		// an "enhanced submission" is simply a submission with a Boolean flag to say whether it has been downloaded
		val enhancedSubmissions = enhancedSubmissionsCommand.apply()
		hasOriginalityReport = enhancedSubmissions.exists(_.submission.hasOriginalityReport)
		val uniIdsWithSubmissionOrFeedback = assignment.getUniIdsWithSubmissionOrFeedback.toSeq.sorted
		val moduleMembers = assignmentService.determineMembershipUsers(assignment)

		awaitingSubmissionExtended =  Option(moduleMembers) match {
			case Some(members) => {
				val unSubmitted =  members.filterNot(member => uniIdsWithSubmissionOrFeedback.contains(member.getUserId))
				val withExtension = unSubmitted.map(member => (member, assignment.findExtension(member.getWarwickId)))
				val result = withExtension.flatMap(pair => pair match {
					case (u, Some(extension)) => List(Pair(u, extension))
					case (u, None) => Nil
				})
				result
			}
			case None => Nil
		}

		awaitingSubmission =  Option(moduleMembers) match{
			case Some(members) => {
				val unSubmitted =  members.filterNot(member => uniIdsWithSubmissionOrFeedback.contains(member.getUserId))
				unSubmitted.filterNot(awaitingSubmissionExtended contains)
			}
			case None => Nil
		}

		// later we may do more complex checks to see if this particular mark scheme workflow requires that feedback is released manually
		// for now all markschemes will require you to release feedback so if one exists for this assignment - provide it
		mustReleaseForMarking = assignment.markScheme != null

		students = for (uniId <- uniIdsWithSubmissionOrFeedback) yield {
			val usersSubmissions = enhancedSubmissions.filter(submissionListItem => submissionListItem.submission.universityId == uniId)
			val usersFeedback = assignment.fullFeedback.filter(feedback => feedback.universityId == uniId)

			val userFilter = moduleMembers.filter(member => member.getWarwickId == uniId)
			val user = if(userFilter.isEmpty) {
				userLookup.getUserByWarwickUniId(uniId)
			} else {
				userFilter.head
			}

			val userFullName = user.getFullName

			val enhancedSubmissionForUniId = usersSubmissions.toList match {
				case head :: Nil => head
				case head :: others => throw new IllegalStateException("More than one SubmissionListItem (" + usersSubmissions.size + ") for " + uniId)
				case Nil => new SubmissionListItem(new Submission(), false)
			}

			if (usersFeedback.size > 1) {
				throw new IllegalStateException("More than one Feedback for " + uniId)
			}

			val feedbackForUniId: Feedback = usersFeedback.headOption.orNull

			new Item(uniId, enhancedSubmissionForUniId, feedbackForUniId, userFullName)
		}

		// True if any feedback exists that's been published. To decide whether to show whoDownloaded count.
		hasPublishedFeedback = students.exists { student =>
			student.feedback != null && student.feedback.checkedReleased
		}
		
		whoDownloaded = auditIndexService.whoDownloadedFeedback(assignment).map(userLookup.getUserByUserId(_))
		val members =  assignmentService.determineMembershipUsers(assignment)
		stillToDownload = (members.toSet -- whoDownloaded.toSet)
	}
}

// Simple object holder
class Item(val uniId: String, val enhancedSubmission: SubmissionListItem, val feedback: Feedback, val fullName: String)
