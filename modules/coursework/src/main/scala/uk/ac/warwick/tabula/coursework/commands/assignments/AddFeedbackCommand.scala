package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.model.{ Assignment, Feedback }
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.permissions._
import org.springframework.validation.Errors

class AddFeedbackCommand(module: Module, assignment: Assignment, submitter: CurrentUser)
	extends UploadFeedbackCommand[List[Feedback]](module, assignment, submitter) {

	PermissionCheck(Permissions.Feedback.Create, assignment)

	override def applyInternal(): List[Feedback] = transactional() {
		def saveFeedback(uniNumber: String, file: UploadedFile) = {
			val feedback = assignment.findFeedback(uniNumber).getOrElse(new Feedback)
			feedback.assignment = assignment
			feedback.uploaderId = submitter.apparentId
			feedback.universityId = uniNumber
			feedback.released = false
			for (attachment <- file.attached) {
				// if an attachment with the same name as this one exists then delete it
				val duplicateAttachment = feedback.attachments.find(_.name == attachment.name)
				duplicateAttachment.foreach(session.delete(_))
				feedback addAttachment attachment
			}
			session.saveOrUpdate(feedback)

			feedback
		}

		if (items != null && !items.isEmpty()) {

			val feedbacks = items.map { (item) =>
				val feedback = saveFeedback(item.uniNumber, item.file)
				zipService.invalidateIndividualFeedbackZip(feedback)
				feedback
			}

			zipService.invalidateFeedbackZip(assignment)
			feedbacks.toList

		} else {

			val feedback = saveFeedback(uniNumber, file)

			// delete feedback zip for this assignment, since it'll now be different.
			// TODO should really do this in a more general place, like a save listener for Feedback objects
			zipService.invalidateFeedbackZip(assignment)

			List(feedback)
		}
	}
	
	override def validateExisting(item: FeedbackItem, errors: Errors) {
		// warn if feedback for this student is already uploaded
		assignment.feedbacks.find { feedback => feedback.universityId == item.uniNumber && feedback.hasAttachments } match {
			case Some(feedback) => {
				// set warning flag for existing feedback and check if any existing files will be overwritten
				item.submissionExists = true
				checkForDuplicateFiles(item, feedback)
			}
			case None => {}
		}
	}

	private def checkForDuplicateFiles(item: FeedbackItem, feedback: Feedback){
		val attachedFiles = item.file.attachedFileNames.toSet
		val feedbackFiles = feedback.attachments.map(file => file.getName).toSet
		item.duplicateFileNames = attachedFiles & feedbackFiles
	}
	
	def describe(d: Description) = d
		.assignment(assignment)
		.studentIds(items.map { _.uniNumber })

}

