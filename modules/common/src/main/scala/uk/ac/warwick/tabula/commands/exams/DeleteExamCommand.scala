package uk.ac.warwick.tabula.commands.exams


import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._

class DeleteExamCommand(val exam: Exam) extends Command[Exam] with SelfValidating {

	PermissionCheck(Permissions.Assignment.Delete, exam)

	var service = Wire[AssessmentService]
	var confirm = false

	override def applyInternal() =  {
		exam.markDeleted()
		service.save(exam)
		exam
	}

	def validate(errors: Errors) {
		if (!confirm) {
			errors.rejectValue("confirm", "exam.delete.confirm")
		} else validateCanDelete(errors)
	}

	def validateCanDelete(errors: Errors) {
		if (exam.deleted) {
			errors.reject("exam.delete.deleted")
		} else if (!exam.feedbacks.isEmpty) {
			errors.reject("exam.delete.marksAssociated")
		}
	}

	override def describe(d: Description) = d.exam(exam)

}