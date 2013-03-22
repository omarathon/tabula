package uk.ac.warwick.tabula.coursework.web.controllers.admin

import scala.collection.JavaConversions._
import javax.validation.Valid
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import scala.collection.JavaConversions.seqAsJavaList
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/new"))
class AddAssignment extends CourseworkController {

	@Autowired var assignmentService: AssignmentService = _

	@ModelAttribute("academicYearChoices") def academicYearChoices: java.util.List[AcademicYear] = {
		AcademicYear.guessByDate(DateTime.now).yearsSurrounding(2, 2)
	}

	validatesWith[AddAssignmentCommand] { (cmd: AddAssignmentCommand, errors) =>
		cmd.validate(errors)
	}

	@ModelAttribute def addAssignmentForm(@PathVariable("module") module: Module) =
		new AddAssignmentCommand(mandatory(module))

	// Used for initial load and for prefilling from a chosen assignment
	@RequestMapping()
	def form(form: AddAssignmentCommand) = {
		form.afterBind()
		form.prefillFromRecentAssignment()
		formView(form)
	}

	// when reloading the form
	@RequestMapping(params = Array("action=refresh"))
	def formRefresh(form: AddAssignmentCommand) = {
		form.afterBind()
		formView(form)
	}

	@RequestMapping(method = Array(POST), params = Array("action=submit"))
	def submit(@Valid form: AddAssignmentCommand, errors: Errors) = {
		form.afterBind()
		if (errors.hasErrors) {
			formView(form)
		} else {
			form.apply()
			Redirect(Routes.admin.module(form.module))
		}
	}

	def formView(form: AddAssignmentCommand) = {
		val module = form.module
		
		Mav("admin/assignments/new",
			"department" -> module.department,
			"module" -> module,
			"upstreamAssessmentGroups" -> form.upstreamAssessmentGroups,
			"linkedAssessmentGroups" -> form.assessmentGroups,
			"maxWordCount" -> Assignment.MaximumWordCount)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/edit"))
class EditAssignment extends CourseworkController {

	validatesWith[EditAssignmentCommand] { (form: EditAssignmentCommand, errors: Errors) =>
		form.validate(errors)
		if (form.academicYear != form.assignment.academicYear) {
			errors.rejectValue("academicYear", "academicYear.immutable")
		}
	}

	@ModelAttribute def formObject(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) =
		new EditAssignmentCommand(module, mandatory(assignment))

	@RequestMapping
	def showForm(form: EditAssignmentCommand) = {
		form.afterBind()

		val (module, assignment) = (form.module, form.assignment)
		
		val couldDelete = canDelete(module, assignment)
		Mav("admin/assignments/edit",
			"department" -> module.department,
			"module" -> module,
			"assignment" -> assignment,
			"canDelete" -> couldDelete,
			"upstreamAssessmentGroups" -> form.upstreamAssessmentGroups,
			"linkedAssessmentGroups" -> form.assessmentGroups,
			"maxWordCount" -> Assignment.MaximumWordCount)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	@RequestMapping(method = Array(RequestMethod.POST), params = Array("action!=refresh"))
	def submit(@Valid form: EditAssignmentCommand, errors: Errors) = {
		if (errors.hasErrors) {
			showForm(form)
		} else {
			form.afterBind()
			form.apply()
			Redirect(Routes.admin.module(form.module))
		}

	}

	private def canDelete(module: Module, assignment: Assignment): Boolean = {
		val cmd = new DeleteAssignmentCommand(module, assignment)
		val errors = new BeanPropertyBindingResult(cmd, "cmd")
		cmd.prechecks(errors)
		!errors.hasErrors
	}

}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/delete"))
class DeleteAssignment extends CourseworkController {

	validatesWith[DeleteAssignmentCommand] { (form: DeleteAssignmentCommand, errors: Errors) =>
		form.validate(errors)
	}

	@ModelAttribute def formObject(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) =
		new DeleteAssignmentCommand(module, mandatory(assignment))

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def showForm(form: DeleteAssignmentCommand) = {
		val (module, assignment) = (form.module, form.assignment)
		
		Mav("admin/assignments/delete",
			"department" -> module.department,
			"module" -> module,
			"assignment" -> assignment,
			"maxWordCount" -> Assignment.MaximumWordCount)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	@RequestMapping(method = Array(RequestMethod.POST))
	def submit(@Valid form: DeleteAssignmentCommand, errors: Errors) = {
		if (errors.hasErrors) {
			showForm(form)
		} else {
			form.apply()
			Redirect(Routes.admin.module(form.module))
		}

	}

}

