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
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.tabula.coursework.commands.assignments.UpstreamGroup
import uk.ac.warwick.tabula.coursework.commands.assignments.UpstreamGroupPropertyEditor
import org.springframework.http.HttpRequest
import org.springframework.http.HttpMethod
import javax.servlet.http.HttpServletRequest

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/new"))
class AddAssignment extends CourseworkController {

	@Autowired var assignmentService: AssignmentService = _

	@ModelAttribute("academicYearChoices") def academicYearChoices: JList[AcademicYear] = {
		AcademicYear.guessByDate(DateTime.now).yearsSurrounding(2, 2)
	}

	validatesSelf[AddAssignmentCommand]

	@ModelAttribute def addAssignmentForm(@PathVariable("module") module: Module) =
		new AddAssignmentCommand(mandatory(module))

	// Used for initial load and for prefilling from a chosen assignment
	@RequestMapping()
	def form(form: AddAssignmentCommand) = {
		form.afterBind()
		form.prefillFromRecentAssignment()
		showForm(form)
	}

	@RequestMapping(method = Array(POST), params = Array("action=submit"))
	def submit(@Valid form: AddAssignmentCommand, errors: Errors) = {
		form.afterBind()
		if (errors.hasErrors) {
			showForm(form)
		} else {
			form.apply()
			Redirect(Routes.admin.module(form.module))
		}
	}

	@RequestMapping(method = Array(POST), params = Array("action=update"))
	def update(@Valid form: AddAssignmentCommand, errors: Errors) = {
		form.afterBind()
		if (errors.hasErrors) {
			showForm(form)
		} else {
			form.apply()
			Redirect(Routes.admin.assignment.edit(form.assignment) + "?open")
		}

	}

	def showForm(form: AddAssignmentCommand) = {
		val module = form.module

		Mav("admin/assignments/new",
			"department" -> module.department,
			"module" -> module,
			"availableUpstreamGroups" -> form.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> form.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> form.assessmentGroups,
			"maxWordCount" -> Assignment.MaximumWordCount)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}
}


@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/edit"))
class EditAssignment extends CourseworkController {

	validatesSelf[EditAssignmentCommand]

	@ModelAttribute def formObject(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) = {
		new EditAssignmentCommand(module, mandatory(assignment))
	}

	@RequestMapping
	def showForm(form: EditAssignmentCommand, openDetails: Boolean = false) = {
		form.afterBind()

		val (module, assignment) = (form.module, form.assignment)
		form.copyGroupsFrom(assignment)

		val couldDelete = canDelete(module, assignment)
		Mav("admin/assignments/edit",
			"department" -> module.department,
			"module" -> module,
			"assignment" -> assignment,
			"canDelete" -> couldDelete,
			"availableUpstreamGroups" -> form.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> form.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> form.assessmentGroups,
			"maxWordCount" -> Assignment.MaximumWordCount,
			"openDetails" -> openDetails)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	@RequestMapping(method = Array(RequestMethod.POST), params = Array("action=submit"))
	def submit(@Valid form: EditAssignmentCommand, errors: Errors) = {
		form.afterBind()
		if (errors.hasErrors) {
			showForm(form)
		} else {
			form.apply()
			Redirect(Routes.admin.module(form.module))
		}

	}

	@RequestMapping(method = Array(RequestMethod.POST), params = Array("action=update"))
	def update(@Valid form: EditAssignmentCommand, errors: Errors) = {
		form.afterBind()
		if (!errors.hasErrors) {
			form.apply()
		}

		showForm(form, true)
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
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

	validatesSelf[DeleteAssignmentCommand]

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


/**
 * Controller to populate the user listing for editing, without persistence
 */
@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/enrolment"))
class AssignmentEnrolment extends CourseworkController {

	validatesSelf[EditAssignmentEnrolmentCommand]

	@ModelAttribute def formObject(@PathVariable("module") module: Module) = {
		val cmd = new EditAssignmentEnrolmentCommand(mandatory(module))
		cmd.upstreamGroups.clear()
		cmd
	}

	@RequestMapping
	def showForm(form: EditAssignmentEnrolmentCommand, openDetails: Boolean = false) = {
		form.afterBind()

		Mav("admin/assignments/enrolment",
			"department" -> form.module.department,
			"module" -> form.module,
			"availableUpstreamGroups" -> form.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> form.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> form.assessmentGroups,
			"openDetails" -> openDetails)
			.noLayout()
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}
}

