package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.tabula.coursework.commands.assignments.ReleaseForMarkingCommand
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.actions.Participate
import org.springframework.validation.Errors
import javax.validation.Valid
import uk.ac.warwick.tabula.coursework.commands.assignments.MarkPlagiarisedCommand


@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissionsandfeedback/release-submissions"))
class ReleaseForMarkingController extends CourseworkController {

	@ModelAttribute
	def command(@PathVariable("assignment") assignment: Assignment) = new ReleaseForMarkingCommand(assignment)

	validatesSelf[MarkPlagiarisedCommand]

	def confirmView(assignment: Assignment) = Mav("admin/assignments/submissionsandfeedback/release-submission",
		"assignment" -> assignment)
		.crumbs(Breadcrumbs.Department(assignment.module.department), Breadcrumbs.Module(assignment.module))

	def RedirectBack(assignment: Assignment) = Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))

	// shouldn't ever be called as a GET - if it is, just redirect back to the submission list
	@RequestMapping(method = Array(GET))
	def get(@PathVariable assignment: Assignment) = RedirectBack(assignment)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(
				@PathVariable("module") module: Module,
				@PathVariable("assignment") assignment: Assignment,
				form: ReleaseForMarkingCommand, errors: Errors) = {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		form.preSubmitValidation()
		confirmView(assignment)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
				@PathVariable("module") module: Module,
				@PathVariable("assignment") assignment: Assignment,
				@Valid form: ReleaseForMarkingCommand, errors: Errors) = {
		transactional() {
			mustBeLinked(assignment, module)
			mustBeAbleTo(Participate(module))
			if (errors.hasErrors)
				confirmView(assignment)
			else {
				form.apply()
				RedirectBack(assignment)
			}
		}
	}

}
