package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.{WorkflowStages, WorkflowStage}

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.coursework.helpers.{CourseworkFilter, CourseworkFilters}
import uk.ac.warwick.tabula.coursework.services.CourseworkWorkflowService
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._

import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.commands.SelfValidating
import org.springframework.validation.Errors
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.coursework.commands.feedback.ListFeedbackCommand
import uk.ac.warwick.tabula.coursework.commands.feedback.FeedbackListItem

import ListSubmissionsCommand.SubmissionListItem
import SubmissionAndFeedbackCommand._

object SubmissionAndFeedbackCommand {
	def apply(module: Module, assignment: Assignment) =
		new SubmissionAndFeedbackCommand(module, assignment)
		with AutowiringAssessmentMembershipServiceComponent
		with AutowiringUserLookupComponent
		with AutowiringFeedbackForSitsServiceComponent

	case class SubmissionAndFeedbackResults (
		students:Seq[Student],
		whoDownloaded: Seq[(User, DateTime)],
		stillToDownload: Seq[Student],
		hasPublishedFeedback: Boolean,
		hasOriginalityReport: Boolean,
		mustReleaseForMarking: Boolean
	)

	// Simple object holder
	case class Student (
		user: User,
		progress: Progress,
		nextStage: Option[WorkflowStage],
		stages: ListMap[String, WorkflowStages.StageProgress],
		coursework: WorkflowItems,
		assignment:Assignment
	)

	case class WorkflowItems (
		student: User,
		enhancedSubmission: Option[SubmissionListItem],
		enhancedFeedback: Option[FeedbackListItem],
		enhancedExtension: Option[ExtensionListItem]
	)

	case class Progress (
		percentage: Int,
		t: String,
		messageCode: String
	)

	case class ExtensionListItem (
		extension: Extension,
		within: Boolean
	)
}

abstract class SubmissionAndFeedbackCommand(val module: Module, val assignment: Assignment)
	extends Command[SubmissionAndFeedbackResults] with Unaudited with ReadOnly with SelfValidating {

	self: AssessmentMembershipServiceComponent with UserLookupComponent with FeedbackForSitsServiceComponent =>
	
	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Submission.Read, assignment)

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
		val hasOriginalityReport = benchmarkTask("Check for originality reports") {
			enhancedSubmissions.exists(_.submission.hasOriginalityReport)
		}
		val uniIdsWithSubmissionOrFeedback = benchmarkTask("Get uni IDs with submissions or feedback") {
			assignment.getUniIdsWithSubmissionOrFeedback.toSeq.sorted
		}
		val moduleMembers = benchmarkTask("Get module membership") {
			assessmentMembershipService.determineMembershipUsers(assignment)
		}
		val unsubmittedMembers = moduleMembers.filterNot(m => uniIdsWithSubmissionOrFeedback.contains(m.getWarwickId))

		def enhancedFeedbackForUniId(uniId: String) = {
			val usersFeedback = assignment.feedbacks.asScala.filter(feedback => feedback.universityId == uniId)
			if (usersFeedback.size > 1) throw new IllegalStateException("More than one Feedback for " + uniId)
			usersFeedback.headOption map { feedback =>
				val downloaded = !feedback.attachments.isEmpty && (whoDownloaded exists { x =>
					x._1.getWarwickId == feedback.universityId &&
						x._2.isAfter(feedback.mostRecentAttachmentUpload)
				})

				val viewed = (feedback.hasOnlineFeedback || feedback.hasGenericFeedback) && (whoViewed exists { x =>
					val universityId = x._1.getWarwickId
					val latestOnlineUpdate =
						latestModifiedOnlineFeedback.find(_._1.getWarwickId == universityId).map {
							_._2
						}.getOrElse(new DateTime(0))
					val latestUpdate = latestGenericFeedbackUpdate.filter(_.isAfter(latestOnlineUpdate)).getOrElse(latestOnlineUpdate)
					universityId == feedback.universityId && x._2.isAfter(latestUpdate)
				})

				FeedbackListItem(feedback, downloaded, viewed, feedbackForSitsService.getByFeedback(feedback).orNull)
			}
		}

		val unsubmitted = benchmarkTask("Get unsubmitted users") {
			for (user <- unsubmittedMembers) yield {
				val usersExtension = assignment.extensions.asScala.filter(_.universityId == user.getWarwickId)
				if (usersExtension.size > 1) throw new IllegalStateException("More than one Extension for " + user.getWarwickId)

				val enhancedExtensionForUniId = usersExtension.headOption map { extension =>
					new ExtensionListItem(
						extension=extension,
						within=assignment.isWithinExtension(user)
					)
				}

				val coursework = WorkflowItems(
					user,
					enhancedSubmission=None,
					enhancedFeedback=enhancedFeedbackForUniId(user.getWarwickId),
					enhancedExtension=enhancedExtensionForUniId
				)

				val progress = courseworkWorkflowService.progress(assignment)(coursework)

				Student(
					user=user,
					progress=Progress(progress.percentage, progress.cssClass, progress.messageCode),
					nextStage=progress.nextStage,
					stages=progress.stages,
					coursework=coursework,
					assignment=assignment
				)
			}
		}

		val submitted = benchmarkTask("Get submitted users") { for (uniId <- uniIdsWithSubmissionOrFeedback) yield {
			val usersSubmissions = enhancedSubmissions.filter(_.submission.universityId == uniId)
			val usersExtension = assignment.extensions.asScala.filter(extension => extension.universityId == uniId)

			val userFilter = moduleMembers.filter(member => member.getWarwickId == uniId)
			val user = if(userFilter.isEmpty) {
				userLookup.getUserByWarwickUniId(uniId)
			} else {
				userFilter.head
			}
			
			if (usersSubmissions.size > 1) throw new IllegalStateException("More than one Submission for " + uniId)
			if (usersExtension.size > 1) throw new IllegalStateException("More than one Extension for " + uniId)

			val enhancedSubmissionForUniId = usersSubmissions.headOption
			
			val enhancedExtensionForUniId = usersExtension.headOption map { extension =>
				new ExtensionListItem(
					extension=extension,
					within=assignment.isWithinExtension(user)
				)
			}
			
			val coursework = WorkflowItems(
				user,
				enhancedSubmission=enhancedSubmissionForUniId, 
				enhancedFeedback=enhancedFeedbackForUniId(uniId),
				enhancedExtension=enhancedExtensionForUniId
			)
			
			val progress = courseworkWorkflowService.progress(assignment)(coursework)

			Student(
				user=user,
				progress=Progress(progress.percentage, progress.cssClass, progress.messageCode),
				nextStage=progress.nextStage,
				stages=progress.stages,
				coursework=coursework,
				assignment=assignment
			)
		}}
		
		val membersWithPublishedFeedback = submitted.filter { student => 
			student.coursework.enhancedFeedback exists { _.feedback.checkedReleased }
		}

		// True if any feedback exists that's been published. To decide whether to show whoDownloaded count.
		val hasPublishedFeedback = membersWithPublishedFeedback.nonEmpty
		
		val stillToDownload = membersWithPublishedFeedback.filterNot(_.coursework.enhancedFeedback.exists(_.downloaded))
		
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
			mustReleaseForMarking=assignment.mustReleaseForMarking
		)
	}
	
	def validate(errors: Errors) {
		Option(filter) foreach { _.validate(filterParameters.asScala.toMap)(errors) }
	}
}