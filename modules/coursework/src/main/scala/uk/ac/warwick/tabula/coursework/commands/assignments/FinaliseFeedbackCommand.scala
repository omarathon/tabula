package uk.ac.warwick.tabula.coursework.commands.assignments

import collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{Feedback, MarkerFeedback, Assignment}
import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.data.{FileDao, Daoisms}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import org.joda.time.DateTime

/**
 * Copies the appropriate MarkerFeedback item to its parent Feedback ready for processing by administrators
 */
class FinaliseFeedbackCommand(val assignment: Assignment, val markerFeedbacks:JList[MarkerFeedback])
	extends Command[Unit] {

	var fileDao = Wire.auto[FileDao]

	PermissionCheck(Permissions.Feedback.Create, assignment)

	def applyInternal() {
		markerFeedbacks.foreach { markerFeedback =>
			this.copyToFeedback(markerFeedback)
		}
	}

	override def describe(d: Description){
		d.assignment(assignment)
		d.property("updatedFeedback" -> markerFeedbacks.size)
	}

	def copyToFeedback(markerFeedback: MarkerFeedback): Feedback = {
		val parent = markerFeedback.feedback

		parent.clearCustomFormValues()

		// save custom fields
		parent.customFormValues.addAll(markerFeedback.customFormValues.map { formValue =>
				val newValue = new SavedFormValue()
				newValue.name = formValue.name
				newValue.feedback = formValue.markerFeedback.feedback
				newValue.value = formValue.value
				newValue
		}.toSet[SavedFormValue])


		parent.actualGrade = markerFeedback.grade
		parent.actualMark = markerFeedback.mark

		parent.uploadedDate = DateTime.now

		// erase any existing attachments - these will be replaced
		parent.clearAttachments()

		markerFeedback.attachments.foreach(parent.addAttachment)
		parent
	}
}
