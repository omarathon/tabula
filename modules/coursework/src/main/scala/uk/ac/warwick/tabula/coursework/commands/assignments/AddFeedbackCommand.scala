package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.model.{MarkingCompleted, Assignment, Feedback}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.actions.Participate

class AddFeedbackCommand(val module: Module, assignment: Assignment, submitter: CurrentUser)
	extends UploadFeedbackCommand[List[Feedback]](assignment, submitter)  {
	
	mustBeLinked(assignment, module)
	PermissionsCheck(Participate(module))

	override def applyInternal(): List[Feedback] = transactional() {

		def saveFeedback(uniNumber: String, file: UploadedFile) = {
			val feedback = assignment.findFeedback(uniNumber).getOrElse(new Feedback)
			feedback.assignment = assignment
			feedback.uploaderId = submitter.apparentId
			feedback.universityId = uniNumber
			feedback.released = false
			for (attachment <- file.attached){
				// if an attachment with the same name as this one exists then delete it
				val duplicateAttachment = feedback.attachments.find(_.name == attachment.name)
				duplicateAttachment.foreach(session.delete(_))
				feedback addAttachment attachment
			}
			session.saveOrUpdate(feedback)
			updateSubmissionState(uniNumber)

			feedback
		}

		def updateSubmissionState(uniNumber: String) {
			val submission = assignmentService.getSubmissionByUniId(assignment, uniNumber)
			submission.foreach(submissionService.updateState(_, MarkingCompleted))
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
def describe(d: Description) = d
		.assignment(assignment)
		.studentIds(items.map { _.uniNumber })

}

