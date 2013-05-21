package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.MarkingState._
import uk.ac.warwick.tabula.data.model.{Feedback, Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{UploadedFile, Description}
import uk.ac.warwick.tabula.data.Transactions._
import reflect.BeanProperty
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.Module
import org.springframework.validation.Errors


class AddMarkerFeedbackCommand(module: Module, assignment:Assignment, submitter: CurrentUser, val firstMarker:Boolean)
	extends UploadFeedbackCommand[List[MarkerFeedback]](module, assignment, submitter)  {
	
	PermissionCheck(Permissions.Feedback.Create, assignment)

	// list to contain feedback files that are not for a student you should be marking
	var invalidStudents: JList[FeedbackItem] = LazyLists.simpleFactory()
	// list to contain feedback files that are  for a student that has already been completed
	var markedStudents: JList[FeedbackItem] = LazyLists.simpleFactory()

	val submissions = assignment.getMarkersSubmissions(submitter.apparentUser)

	def processStudents() {
		val markedSubmissions = submissions.filter{ submission =>
			val markerFeedback =  assignment.getMarkerFeedback(submission.universityId, submitter.apparentUser)
			markerFeedback match {
				case Some(f) if f.state != MarkingCompleted => true
				case _ => false
			}
		}
		val universityIds = submissions.map(_.universityId)
		val markedIds = markedSubmissions.map(_.universityId)
		invalidStudents = items.filter(item => !universityIds.contains(item.uniNumber))
		markedStudents = items.filter(item => !markedIds.contains(item.uniNumber))
		items = (items.toList.diff(invalidStudents.toList)).diff(markedStudents.toList)
	}

	private def saveMarkerFeedback(uniNumber: String, file: UploadedFile) = {
		// find the parent feedback or make a new one
		val parentFeedback = assignment.feedbacks.find(_.universityId == uniNumber).getOrElse({
			val newFeedback = new Feedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = submitter.apparentId
			newFeedback.universityId = uniNumber
			newFeedback.released = false
			newFeedback
		})

		// see if marker feedback already exists - if not create one
		val markerFeedback:MarkerFeedback = firstMarker match {
			case true => parentFeedback.retrieveFirstMarkerFeedback
			case false => parentFeedback.retrieveSecondMarkerFeedback
		}

		for (attachment <- file.attached){
			// if an attachment with the same name as this one exists then delete it
			val duplicateAttachment = markerFeedback.attachments.find(_.name == attachment.name)
			duplicateAttachment.foreach(markerFeedback.removeAttachment(_))
			markerFeedback.addAttachment(attachment)
		}

		session.saveOrUpdate(parentFeedback)
		session.saveOrUpdate(markerFeedback)
		//TODO - UPDATE STATE

		markerFeedback
	}

	override def applyInternal(): List[MarkerFeedback] = transactional() {
		if (items != null && !items.isEmpty) {
			val markerFeedbacks = items.map { (item) =>
				val feedback = saveMarkerFeedback(item.uniNumber, item.file)
				feedback
			}
			markerFeedbacks.toList
		} else {
			val markerFeedbacks = saveMarkerFeedback(uniNumber, file)
			List(markerFeedbacks)
		}
	}
	
	override def validateExisting(item: FeedbackItem, errors: Errors) {
		def toMarkerFeedback(feedback: Feedback) = 
			if (firstMarker) Option(feedback.firstMarkerFeedback)
			else Option(feedback.secondMarkerFeedback)
		
		// warn if feedback for this student is already uploaded
		assignment.feedbacks.find { feedback => feedback.universityId == item.uniNumber } flatMap { toMarkerFeedback(_) } match {
			case Some(markerFeedback) if markerFeedback.hasFeedback => {
				// set warning flag for existing feedback and check if any existing files will be overwritten
				item.submissionExists = true
				checkForDuplicateFiles(item, markerFeedback)
			}
			case _ => {}
		}
	}

	private def checkForDuplicateFiles(item: FeedbackItem, feedback: MarkerFeedback){
		val attachedFiles = item.file.attachedFileNames.toSet
		val feedbackFiles = feedback.attachments.map(file => file.getName).toSet
		item.duplicateFileNames = attachedFiles & feedbackFiles
	}

	def describe(d: Description){
		d.assignment(assignment)
		 .studentIds(items.map { _.uniNumber })
	}
}
