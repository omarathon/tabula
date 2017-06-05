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

//FIXME - Ideally should use cake pattern  for this and UploadFeedbackCommand but leaving it as it is and using existing cm1 command code
class AddMarkerFeedbackCommand(assignment: Assignment, marker: User, val submitter: CurrentUser)
	extends UploadFeedbackCommand[List[MarkerFeedback]](assignment, marker) with CanProxy {

	PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
	if (submitter.apparentUser != marker) {
		PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
	}

	// list to contain feedback files that are not for a student you should be marking
	var invalidStudents: JList[FeedbackItem] = LazyLists.create[FeedbackItem]()

	// list to contain feedback files that are  for a student that has already been completed
	var markedStudents: JList[FeedbackItem] = LazyLists.create[FeedbackItem]()


	def processStudents() {
		val allMarkerFeedbacks = assignment.allFeedback.flatMap { feedback =>
			val outstandingStages = feedback.outstandingStages.asScala
			val mFeedback = feedback.allMarkerFeedback.filter { mFeedback =>
				mFeedback.marker == marker && (outstandingStages.contains(mFeedback.stage) || mFeedback.feedback.isMarkingCompleted)
			}
			mFeedback
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
		val markerFeedback = parentFeedback.allMarkerFeedback.find(mf => mf.marker == marker && parentFeedback.outstandingStages.contains(mf.stage)).getOrElse(throw new IllegalArgumentException)


		for (attachment <- file.attached.asScala) {
			// if an attachment with the same name as this one exists then delete it
			val duplicateAttachment = markerFeedback.attachments.asScala.find(_.name == attachment.name)
			duplicateAttachment.foreach(markerFeedback.removeAttachment)
			markerFeedback.addAttachment(attachment)
		}

		parentFeedback.updatedDate = DateTime.now
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
		val feedback = usercode.flatMap(u => assignment.allFeedback.find(_.usercode == u))
		val allMarkerFeedback = feedback.flatMap { feedback =>
			val outstandingStages = feedback.outstandingStages.asScala
			feedback.allMarkerFeedback.find(mf => mf.marker == marker && (outstandingStages.contains(mf.stage) || mf.feedback.isMarkingCompleted))
		} match {
			case Some(markerFeedback) if markerFeedback.hasFeedback =>
				// set warning flag for existing feedback and check if any existing files will be overwritten
				item.submissionExists = true
				checkForDuplicateFiles(item, markerFeedback)
			case Some(markerFeedback) =>
			case _ => errors.reject("markingWorkflow.feedback.finalised", "No more feedback can be added")
		}
	}

	private def checkForDuplicateFiles(item: FeedbackItem, feedback: MarkerFeedback) {
		val attachedFiles = item.file.attachedFileNames.toSet
		val feedbackFiles = feedback.attachments.asScala.map(file => file.getName).toSet
		item.duplicateFileNames = attachedFiles & feedbackFiles
	}

	def describe(d: Description) {
		d.assignment(assignment)
			.studentIds(items.asScala.map {
				_.uniNumber
			})
			.studentUsercodes(items.asScala.flatMap(_.student.map(_.getUserId)))
	}

	override def describeResult(d: Description, feedbacks: List[MarkerFeedback]): Unit = {
		d.assignment(assignment)
			.studentIds(items.asScala.map {
				_.uniNumber
			})
			.studentUsercodes(items.asScala.flatMap(_.student.map(_.getUserId)))
			.fileAttachments(feedbacks.flatMap {
				_.attachments.asScala
			})
			.properties("feedback" -> feedbacks.map {
				_.id
			})
	}
}
