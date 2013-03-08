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
import uk.ac.warwick.tabula.coursework.commands.feedback.FeedbackListItem
import uk.ac.warwick.tabula.coursework.commands.feedback.FeedbackListItem
import org.joda.time.DateTime

class SubmissionAndFeedbackCommand(val module: Module, val assignment: Assignment) extends Command[Unit] with Unaudited with ReadOnly {
	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Submission.Read, assignment)

	var auditIndexService = Wire.auto[AuditEventIndexService]
	var assignmentService = Wire.auto[AssignmentService]
	var userLookup = Wire.auto[UserLookupService]

	@BeanProperty var students:Seq[Item] = _
	@BeanProperty var awaitingSubmissionExtended:Seq[(User, Extension)] = _
	@BeanProperty var awaitingSubmissionWithinExtension:Seq[(User, Extension)] = _
	@BeanProperty var awaitingSubmissionExtensionRequested:Seq[(User, Extension)] = _
	@BeanProperty var awaitingSubmissionExtensionExpired:Seq[(User, Extension)] = _
	@BeanProperty var awaitingSubmissionExtensionRejected:Seq[(User, Extension)] = _
	@BeanProperty var awaitingSubmissionNoExtension:Seq[User] = _
	@BeanProperty var whoDownloaded: Seq[(User, DateTime)] = _
	@BeanProperty var stillToDownload: Seq[Item] =_
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
		val unSubmitted =  moduleMembers.filterNot(member => uniIdsWithSubmissionOrFeedback.contains(member.getWarwickId))
		val withExtension = unSubmitted.map(member => (member, assignment.findExtension(member.getWarwickId)))
		
		awaitingSubmissionExtended =  Option(moduleMembers) match {
			case Some(members) => {
				val result = withExtension.flatMap(pair => pair match {
					case (u, Some(extension)) => List(Pair(u, extension))
					case (u, None) => Nil
				})
				result
			}
			case None => Nil
		}
		
		awaitingSubmissionNoExtension =  Option(moduleMembers) match {
			case Some(members) => {
				val result = withExtension.flatMap(pair => pair match {
					case (u, Some(extension)) => Nil
					case (u, None) => List(u)
				})
				result
			}
			case None => Nil
		}
		
		awaitingSubmissionWithinExtension = awaitingSubmissionExtended.filter(member => assignment.isWithinExtension(member._1.getUserId))
		
		awaitingSubmissionExtensionRequested = awaitingSubmissionExtended.filter(member => member._2.isAwaitingApproval)
		
		awaitingSubmissionExtensionRejected = awaitingSubmissionExtended.filter(member => member._2.rejected)
		
		awaitingSubmissionExtensionExpired = awaitingSubmissionExtended.filterNot(awaitingSubmissionExtensionRequested contains).filterNot(awaitingSubmissionWithinExtension contains).filterNot(awaitingSubmissionExtensionRejected contains)
		
		// later we may do more complex checks to see if this particular markingWorkflow requires that feedback is released manually
		// for now all markingWorkflow will require you to release feedback so if one exists for this assignment - provide it
		mustReleaseForMarking = assignment.markingWorkflow != null
		
		whoDownloaded = auditIndexService.feedbackDownloads(assignment).map(x =>{(userLookup.getUserByUserId(x._1), x._2)})
		
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

			val enhancedFeedbackForUniId = usersFeedback.headOption match {
				case Some(feedback) => {
					new FeedbackListItem(feedback, whoDownloaded exists { x=> (x._1.getWarwickId == feedback.universityId  &&
							x._2.isAfter(feedback.mostRecentAttachmentUpload))})
				}
				case _ => null
			}

			new Item(uniId, enhancedSubmissionForUniId, enhancedFeedbackForUniId, userFullName)
		}
		
		val membersWithPublishedFeedback = students.filter { student => 
			student.enhancedFeedback != null && student.enhancedFeedback.feedback.checkedReleased
		}

		// True if any feedback exists that's been published. To decide whether to show whoDownloaded count.
		hasPublishedFeedback = !membersWithPublishedFeedback.isEmpty
		
		stillToDownload = membersWithPublishedFeedback filter { !_.enhancedFeedback.downloaded }
	}
}

// Simple object holder
class Item(val uniId: String, val enhancedSubmission: SubmissionListItem, val enhancedFeedback: FeedbackListItem, val fullName: String)
