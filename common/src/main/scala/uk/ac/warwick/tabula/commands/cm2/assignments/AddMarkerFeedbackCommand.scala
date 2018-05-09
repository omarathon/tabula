package uk.ac.warwick.tabula.commands.cm2.assignments

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.CanProxy
import uk.ac.warwick.tabula.commands.cm2.feedback.{FeedbackItem, UploadFeedbackCommand}
import uk.ac.warwick.tabula.commands.{Description, UploadedFile}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

//FIXME - Ideally should use cake pattern  for this and UploadFeedbackCommand - currently used migrated cm1 commands
class AddMarkerFeedbackCommand(assignment: Assignment, marker: User, val submitter: CurrentUser)
	extends UploadFeedbackCommand[List[MarkerFeedback]](assignment, marker) with CanProxy {

	override lazy val eventName = "AddMarkerFeedback"

	PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
	if (submitter.apparentUser != marker) {
		PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
	}

	// list to contain feedback files that are not for a student you should be marking
	var invalidStudents: JList[FeedbackItem] = LazyLists.create[FeedbackItem]()

	// list to contain feedback files that are  for a student that has already been completed
	var markedStudents: JList[FeedbackItem] = LazyLists.create[FeedbackItem]()


	def processStudents() {
		//bring all feedbacks belonging to the current marker along with  completed marking. Completed marking records are displayed to the user at front end if marker did upload again
		val allMarkerFeedbacks = assignment.allFeedback.flatMap { feedback =>
			 feedback.allMarkerFeedback.filter { mFeedback =>
				mFeedback.marker == marker && (mFeedback.outstanding || mFeedback.feedback.isMarkingCompleted)
			}
		}
		val allMarkedFeedbackUserCodes = allMarkerFeedbacks.filter(_.feedback.isMarkingCompleted).map(_.student.getUserId)


		val allMarkerFeedbackUsercodes = allMarkerFeedbacks.map(_.student.getUserId)

		invalidStudents = items.asScala.filter(item => {
			val usercode = item.student.map(_.getUserId).getOrElse("")
			!allMarkerFeedbackUsercodes.contains(usercode)
		}).asJava

		markedStudents = items.asScala.filter(item => {
			val usercode = item.student.map(_.getUserId).getOrElse("")
			allMarkedFeedbackUserCodes.contains(usercode)
		}).asJava
		items = items.asScala.toList.diff(invalidStudents.asScala.toList).diff(markedStudents.asScala.toList).asJava

	}


	private def saveMarkerFeedback(student: User, file: UploadedFile) = {
		// find the parent feedback
		val parentFeedback = assignment.allFeedback.find(_.usercode == student.getUserId).getOrElse(throw new IllegalArgumentException)
		val markerFeedback = parentFeedback.allMarkerFeedback.find(mf => mf.marker == marker && mf.outstanding).getOrElse(throw new IllegalArgumentException)


		for (attachment <- file.attached.asScala) {
			// if an attachment with the same name as this one exists then delete it
			val duplicateAttachment = markerFeedback.attachments.asScala.find(_.name == attachment.name)
			duplicateAttachment.foreach(markerFeedback.removeAttachment)
			markerFeedback.addAttachment(attachment)
		}

		parentFeedback.updatedDate = DateTime.now
		markerFeedback.updatedOn = DateTime.now
		session.saveOrUpdate(parentFeedback)
		session.saveOrUpdate(markerFeedback)
		markerFeedback
	}


	override def applyInternal(): List[MarkerFeedback] = transactional() {
		val markerFeedbacks = for (item <- items.asScala; student <- item.student) yield saveMarkerFeedback(student, item.file)
		markerFeedbacks.toList
	}


	override def validateExisting(item: FeedbackItem, errors: Errors) {
		val usercode = item.student.map(_.getUserId)
		val feedback = usercode.flatMap(assignment.findFeedback)

		val markerFeedback = feedback.toSeq.flatMap(_.allMarkerFeedback)
		val nonFinalFeedback = markerFeedback.filterNot(_.finalised)

		if (nonFinalFeedback.filter(_.marker == marker).exists(_.hasFeedback)) {
			// There is a non-final feedback item, but it's already got something in it.  Warn
			item.submissionExists = true
			checkForDuplicateFiles(item, nonFinalFeedback.filter(_.marker == marker).find(_.hasFeedback).head)
		} else if (markerFeedback.forall(_.marker != marker)) {
			// This marker is not a marker for this student.  Validation error
			errors.reject("markingWorkflow.feedback.otherMarker", Array(usercode.getOrElse("this student")), "You are not assigned to provide feedback for {0}")
		} else if (nonFinalFeedback.isEmpty) {
			// There is no non-final feedback item for anyone.  The feedback is finalised.  Validation error
			errors.reject("markingWorkflow.feedback.finalised", Array(usercode.getOrElse("this student")), "Your feedback for {0} is already finalised")
		}
	}

	private def checkForDuplicateFiles(item: FeedbackItem, feedback: MarkerFeedback) {
		val attachedFiles = item.file.attachedFileNames.toSet
		val feedbackFiles = feedback.attachments.asScala.map(file => file.getName).toSet
		item.duplicateFileNames = attachedFiles & feedbackFiles
	}

	def describe(d: Description) {
		d.assignment(assignment)
			.studentIds(items.asScala.map(_.uniNumber))
			.studentUsercodes(items.asScala.flatMap(_.student.map(_.getUserId)))
	}

	override def describeResult(d: Description, feedbacks: List[MarkerFeedback]): Unit = {
		d.assignment(assignment)
			.studentIds(items.asScala.map(_.uniNumber))
			.studentUsercodes(items.asScala.flatMap(_.student.map(_.getUserId)))
			.fileAttachments(feedbacks.flatMap(_.attachments.asScala))
			.properties("feedback" -> feedbacks.map(_.id))
	}
}
