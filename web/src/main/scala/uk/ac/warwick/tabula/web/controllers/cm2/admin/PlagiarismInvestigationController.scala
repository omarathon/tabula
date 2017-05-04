package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.{PlagiarismInvestigationCommand, PlagiarismInvestigationCommandValidation}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.{CourseworkBreadcrumbs, CourseworkController}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/submissionsandfeedback/mark-plagiarised"))
class PlagiarismInvestigationController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment) = PlagiarismInvestigationCommand(assignment, user.apparentUser)

	validatesSelf[PlagiarismInvestigationCommandValidation]

	def formView(assignment: Assignment): Mav =
		Mav(s"$urlPrefix/admin/assignments/submissionsandfeedback/mark-plagiarised",
				"assignment" -> assignment
		).crumbs(CourseworkBreadcrumbs.Plagiarism.PlagiarismInvestigation(assignment.module))

	def RedirectBack(assignment: Assignment) = Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))

	// shouldn't ever be called as a GET - if it is, just redirect back to the submission list
	@RequestMapping(method = Array(GET))
	def get(@PathVariable assignment: Assignment) = RedirectBack(assignment)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@ModelAttribute("command") form: Appliable[Unit], errors: Errors): Mav = {
		formView(assignment)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@Valid @ModelAttribute("command") form: Appliable[Unit], errors: Errors): Mav = {
		if (errors.hasErrors) {
			formView(assignment)
		} else {
			form.apply()
			RedirectBack(assignment)
		}
	}
}
