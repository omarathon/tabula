package uk.ac.warwick.courses.web.controllers

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.commands.assignments.SendSubmissionReceiptCommand
import uk.ac.warwick.courses.web.{ Mav, Routes }
import uk.ac.warwick.courses.CurrentUser

@Configurable
@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}/resend-receipt"))
class ResendSubmissionEmail extends AbstractAssignmentController {

	hideDeletedItems

	@RequestMapping(method = Array(GET, HEAD))
	def nope(form: SendSubmissionReceiptCommand) = Redirect(Routes.assignment(mandatory(form.assignment)))

	@RequestMapping(method = Array(POST))
	def sendEmail(user: CurrentUser, form: SendSubmissionReceiptCommand): Mav = {
		form.user = user
		mustBeLinked(mandatory(form.assignment), mandatory(form.module))

		val submission = assignmentService.getSubmissionByUniId(form.assignment, user.universityId)
		val hasEmail = user.email.hasText
		val sent: Boolean = submission match {
			case Some(submission) if (submission.submitted) =>
				form.submission = submission
				form.apply()
			case None => false
		}
		Mav("submit/receipt",
			"submission" -> submission,
			"module" -> form.module,
			"assignment" -> form.assignment,
			"sent" -> sent,
			"hasEmail" -> hasEmail)

	}

}
