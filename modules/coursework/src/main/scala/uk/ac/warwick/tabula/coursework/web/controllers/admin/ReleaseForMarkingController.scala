package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}

import javax.validation.Valid
import uk.ac.warwick.tabula.coursework.commands.assignments.MarkPlagiarisedCommand
import uk.ac.warwick.tabula.coursework.commands.assignments.ReleaseForMarkingCommand
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Module, Assignment}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissionsandfeedback/release-submissions"))
class ReleaseForMarkingController extends CourseworkController {

	@ModelAttribute
	def command(@PathVariable("module") module: Module,
				@PathVariable("assignment") assignment: Assignment
				) = new ReleaseForMarkingCommand(module, assignment)

	validatesSelf[MarkPlagiarisedCommand]

	def confirmView(assignment: Assignment) = Mav("admin/assignments/submissionsandfeedback/release-submission",
		"assignment" -> assignment)
		.crumbs(Breadcrumbs.Department(assignment.module.department), Breadcrumbs.Module(assignment.module))

	def RedirectBack(assignment: Assignment) = Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))

	// shouldn't ever be called as a GET - if it is, just redirect back to the submission list
	@RequestMapping(method = Array(GET))
	def get(@PathVariable("assignment") assignment: Assignment) = RedirectBack(assignment)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(cmd: ReleaseForMarkingCommand, errors: Errors) = {
		cmd.preSubmitValidation()
		confirmView(cmd.assignment)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(@Valid cmd: ReleaseForMarkingCommand, errors: Errors) = {
		transactional() {
			if (errors.hasErrors)
				confirmView(cmd.assignment)
			else {
				cmd.apply()
				RedirectBack(cmd.assignment)
			}
		}
	}

}
