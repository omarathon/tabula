package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.coursework.commands.assignments.SendSubmissionReceiptCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.spring.Wire
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.SubmissionService

@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}/resend-receipt"))
class ResendSubmissionEmail extends CourseworkController {
	
	var submissionService = Wire.auto[SubmissionService]

	hideDeletedItems
	
	@ModelAttribute def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) = 
		new SendSubmissionReceiptCommand(
			module, assignment, 
			mandatory(submissionService.getSubmissionByUniId(assignment, user.universityId).filter(_.submitted)), 
			user)

	@RequestMapping(method = Array(GET, HEAD))
	def nope(form: SendSubmissionReceiptCommand) = Redirect(Routes.assignment(mandatory(form.assignment)))

	@RequestMapping(method = Array(POST))
	def sendEmail(form: SendSubmissionReceiptCommand): Mav = {
		val sent = form.apply()
		
		Mav("submit/receipt",
			"submission" -> form.submission,
			"module" -> form.module,
			"assignment" -> form.assignment,
			"sent" -> sent,
			"hasEmail" -> user.email.hasText)

	}

}
