package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.model.{FileAttachment, Assignment, Feedback, Module}
import uk.ac.warwick.tabula.permissions._
import org.springframework.validation.Errors

class AddFeedbackCommand(module: Module, assignment: Assignment, submitter: CurrentUser)
	extends UploadFeedbackCommand[List[Feedback]](module, assignment, submitter) {

	PermissionCheck(Permissions.Feedback.Create, assignment)

	override def applyInternal(): List[Feedback] = transactional() {

		def saveFeedback(uniNumber: String, file: UploadedFile):Option[Feedback] = {
			val feedback = assignment.findFeedback(uniNumber).getOrElse({
				val newFeedback = new Feedback
				newFeedback.assignment = assignment
				newFeedback.uploaderId = submitter.apparentId
				newFeedback.universityId = uniNumber
				newFeedback.released = false
				newFeedback
			})

			val newAttachments = file.attached.filter {attachment =>
				val isIdentical = feedback.attachments.exists(f => f.name == attachment.name && f.isDataEqual(attachment))
				if (!isIdentical){
					// if an attachment with the same name as this one exists then delete it
					val duplicateAttachment = feedback.attachments.find(_.name == attachment.name)
					duplicateAttachment.foreach(session.delete(_))
					feedback addAttachment attachment
				}
				!isIdentical
			}

			if (newAttachments.nonEmpty) {
				session.saveOrUpdate(feedback)
				Some(feedback)
			} else {
				None
			}
		}

		val updatedFeedback = if (items != null && !items.isEmpty()) {
			val feedbacks = items.map { (item) =>
				val feedback = saveFeedback(item.uniNumber, item.file)
				feedback.foreach(zipService.invalidateIndividualFeedbackZip(_))
				feedback
			}.toList.flatten
			zipService.invalidateFeedbackZip(assignment)
			feedbacks
		} else {
			val feedback = saveFeedback(uniNumber, file)
			// delete feedback zip for this assignment, since it'll now be different.
			// TODO should really do this in a more general place, like a save listener for Feedback objects
			zipService.invalidateFeedbackZip(assignment)
			feedback.toList
		}

		updatedFeedback.filter(_.released).foreach {feedback =>
			transactional() {
				val student = userLookup.getUserByWarwickUniId(feedback.universityId)
				val notifyFeedbackChanged = new FeedbackChangeNotifyCommand(module, assignment, student)
				notifyFeedbackChanged.apply()
			}
		}

		updatedFeedback
	}
	
	override def validateExisting(item: FeedbackItem, errors: Errors) {
		// warn if feedback for this student is already uploaded
		assignment.feedbacks.find { feedback => feedback.universityId == item.uniNumber && feedback.hasAttachments } match {
			case Some(feedback) => {
				// set warning flag for existing feedback and check if any existing files will be overwritten
				item.submissionExists = true
				item.isPublished = feedback.released
				checkForDuplicateFiles(item, feedback)
			}
			case None => {}
		}
	}

	private def checkForDuplicateFiles(item: FeedbackItem, feedback: Feedback){
		case class duplicatePair(attached:FileAttachment, feedback:FileAttachment)

		val attachmentNames = item.file.attached.map(_.name)
		val withSameName = for (
			attached <- item.file.attached;
			feedback <- feedback.attachments
			if (attached.name == feedback.name)
		) yield new duplicatePair(attached, feedback)

		item.duplicateFileNames = withSameName.filterNot(p => p.attached.isDataEqual(p.feedback)).map(_.attached.name).toSet
		item.ignoredFileNames = withSameName.map(_.attached.name).toSet -- item.duplicateFileNames
		item.isModified =  (attachmentNames -- item.ignoredFileNames).nonEmpty
	}
	
	def describe(d: Description) = d
		.assignment(assignment)
		.studentIds(items.map { _.uniNumber })

}

