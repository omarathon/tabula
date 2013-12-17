package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.coursework.commands.feedback.FeedbackListItem
import uk.ac.warwick.tabula.coursework.helpers.{CourseworkFilter, CourseworkFilters}
import uk.ac.warwick.tabula.coursework.services.{CourseworkWorkflowService, WorkflowStage, WorkflowStages}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.{AssignmentMembershipService, UserLookupService, AuditEventIndexService}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.commands.SelfValidating
import org.springframework.validation.Errors
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.coursework.commands.feedback.ListFeedbackCommand
import uk.ac.warwick.tabula.helpers.Stopwatches._

class SubmissionAndFeedbackCommand(val module: Module, val assignment: Assignment) 
	extends Command[SubmissionAndFeedbackResults] with Unaudited with ReadOnly with SelfValidating {
	
	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Submission.Read, assignment)

	var assignmentMembershipService = Wire.auto[AssignmentMembershipService]
	var userLookup = Wire.auto[UserLookupService]
	var courseworkWorkflowService = Wire.auto[CourseworkWorkflowService]

	val enhancedSubmissionsCommand = new ListSubmissionsCommand(module, assignment)
	val enhancedFeedbacksCommand = new ListFeedbackCommand(module, assignment)
	
	@NotNull var filter: CourseworkFilter = CourseworkFilters.AllStudents
	var filterParameters: JMap[String, String] = JHashMap()
	
	// When we call export commands, we may want to further filter by a subset of student IDs
	var students: JList[String] = JArrayList()

	def applyInternal() = {
		// an "enhanced submission" is simply a submission with a Boolean flag to say whether it has been downloaded
		val enhancedSubmissions = enhancedSubmissionsCommand.apply()
		val enhancedFeedbacks = enhancedFeedbacksCommand.apply()
		val latestModifiedOnlineFeedback = enhancedFeedbacks.latestOnlineAdded
		val whoDownloaded = enhancedFeedbacks.downloads
		val whoViewed = enhancedFeedbacks.latestOnlineViews
		val latestGenericFeedbackUpdate = enhancedFeedbacks.latestGenericFeedback
		
		val hasOriginalityReport = benchmarkTask("Check for originality reports") { enhancedSubmissions.exists(_.submission.hasOriginalityReport) }
		val uniIdsWithSubmissionOrFeedback = benchmarkTask("Get uni IDs with submissions or feedback") { assignment.getUniIdsWithSubmissionOrFeedback.toSeq.sorted }
		val moduleMembers = benchmarkTask("Get module membership") { assignmentMembershipService.determineMembershipUsers(assignment) }
		val unsubmittedMembers = moduleMembers.filterNot(member => uniIdsWithSubmissionOrFeedback.contains(member.getWarwickId))
		val withExtension = unsubmittedMembers.map(member => (member, assignment.findExtension(member.getWarwickId)))
		
		// later we may do more complex checks to see if this particular markingWorkflow requires that feedback is released manually
		// for now all markingWorkflow will require you to release feedback so if one exists for this assignment - provide it
		val mustReleaseForMarking = assignment.markingWorkflow != null
		
		val unsubmitted = benchmarkTask("Get unsubmitted users") { for (user <- unsubmittedMembers) yield {			
			val usersExtension = assignment.extensions.asScala.filter(extension => extension.universityId == user.getWarwickId)
			
			if (usersExtension.size > 1) throw new IllegalStateException("More than one Extension for " + user.getWarwickId)
			
			val enhancedExtensionForUniId = usersExtension.headOption map { extension =>
				new ExtensionListItem(
					extension=extension,
					within=assignment.isWithinExtension(user.getUserId)
				)
			}
						
			val coursework = WorkflowItems(
				enhancedSubmission=None, 
				enhancedFeedback=None,
				enhancedExtension=enhancedExtensionForUniId
			)
			
			val progress = courseworkWorkflowService.progress(assignment)(coursework)
			
			Student(
				user=user,
				progress=Progress(progress.percentage, progress.cssClass, progress.messageCode),
				nextStage=progress.nextStage,
				stages=progress.stages,
				coursework=coursework
			)
		}}
		val submitted = benchmarkTask("Get submitted users") { for (uniId <- uniIdsWithSubmissionOrFeedback) yield {
			val usersSubmissions = enhancedSubmissions.filter(submissionListItem => submissionListItem.submission.universityId == uniId)
			val usersFeedback = assignment.feedbacks.asScala.filter(feedback => feedback.universityId == uniId)
			val usersExtension = assignment.extensions.asScala.filter(extension => extension.universityId == uniId)

			val userFilter = moduleMembers.filter(member => member.getWarwickId == uniId)
			val user = if(userFilter.isEmpty) {
				userLookup.getUserByWarwickUniId(uniId)
			} else {
				userFilter.head
			}
			
			if (usersSubmissions.size > 1) throw new IllegalStateException("More than one Submission for " + uniId)
			if (usersFeedback.size > 1) throw new IllegalStateException("More than one Feedback for " + uniId)
			if (usersExtension.size > 1) throw new IllegalStateException("More than one Extension for " + uniId)

			val enhancedSubmissionForUniId = usersSubmissions.headOption

			val enhancedFeedbackForUniId = usersFeedback.headOption map { feedback =>
				val downloaded = !feedback.attachments.isEmpty && (whoDownloaded exists { x=> (x._1.getWarwickId == feedback.universityId  &&
					x._2.isAfter(feedback.mostRecentAttachmentUpload))})

				val viewed = (feedback.hasOnlineFeedback || feedback.hasGenericFeedback) && (whoViewed exists { x =>
					val universityId = x._1.getWarwickId
					val latestOnlineUpdate = Option(latestModifiedOnlineFeedback.filter( x => x._1.getWarwickId == universityId).head._2).getOrElse(new DateTime(0))
					val latestUpdate = latestGenericFeedbackUpdate.filter(_.isAfter(latestOnlineUpdate)).getOrElse(latestOnlineUpdate)

					(universityId == feedback.universityId  && 	x._2.isAfter(latestUpdate))})

				new FeedbackListItem(feedback, downloaded, viewed)
			}
			
			val enhancedExtensionForUniId = usersExtension.headOption map { extension =>
				new ExtensionListItem(
					extension=extension,
					within=assignment.isWithinExtension(user.getUserId)
				)
			}
			
			val coursework = WorkflowItems(
				enhancedSubmission=enhancedSubmissionForUniId, 
				enhancedFeedback=enhancedFeedbackForUniId,
				enhancedExtension=enhancedExtensionForUniId
			)
			
			val progress = courseworkWorkflowService.progress(assignment)(coursework)

			Student(
				user=user,
				progress=Progress(progress.percentage, progress.cssClass, progress.messageCode),
				nextStage=progress.nextStage,
				stages=progress.stages,
				coursework=coursework
			)
		}}
		
		val membersWithPublishedFeedback = submitted.filter { student => 
			student.coursework.enhancedFeedback map { _.feedback.checkedReleased } getOrElse (false)
		}

		// True if any feedback exists that's been published. To decide whether to show whoDownloaded count.
		val hasPublishedFeedback = !membersWithPublishedFeedback.isEmpty
		
		val stillToDownload = membersWithPublishedFeedback filterNot { item => item.coursework.enhancedFeedback map { _.downloaded } getOrElse(false) }
		
		val studentsFiltered = benchmarkTask("Do filtering") { 
			val allStudents = (unsubmitted ++ submitted).filter(filter.predicate(filterParameters.asScala.toMap))
			val studentsFiltered = 
				if (students.isEmpty) allStudents
				else allStudents.filter { student => students.contains(student.user.getWarwickId) }
			
			studentsFiltered
		}
		
		SubmissionAndFeedbackResults(
			students=studentsFiltered,
			whoDownloaded=whoDownloaded,
			stillToDownload=stillToDownload,
			hasPublishedFeedback=hasPublishedFeedback,
			hasOriginalityReport=hasOriginalityReport,
			mustReleaseForMarking=mustReleaseForMarking
		)
	}
	
	def validate(errors: Errors) {
		Option(filter) map { _.validate(filterParameters.asScala.toMap)(errors) }
	}
}

case class SubmissionAndFeedbackResults(
	val students:Seq[Student],
	val whoDownloaded: Seq[(User, DateTime)],
	val stillToDownload: Seq[Student],
	val hasPublishedFeedback: Boolean,
	val hasOriginalityReport: Boolean,
	val mustReleaseForMarking: Boolean
)

// Simple object holder
case class Student(
	val user: User,
	val progress: Progress,
	val nextStage: Option[WorkflowStage],
	val stages: ListMap[String, WorkflowStages.StageProgress],
	val coursework: WorkflowItems
)

case class WorkflowItems(
	val enhancedSubmission: Option[SubmissionListItem], 
	val enhancedFeedback: Option[FeedbackListItem],
	val enhancedExtension: Option[ExtensionListItem]
)

case class Progress(
	val percentage: Int,
	val t: String,
	val messageCode: String
)

case class ExtensionListItem(
	val extension: Extension,
	val within: Boolean
)
