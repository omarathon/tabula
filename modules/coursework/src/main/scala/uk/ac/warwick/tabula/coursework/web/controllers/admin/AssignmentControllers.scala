package uk.ac.warwick.tabula.coursework.web.controllers.admin

import javax.persistence.Entity
import javax.persistence.NamedQueries
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.seqAsJavaList
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.actions.Manage
import uk.ac.warwick.tabula.actions.Participate
import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.coursework.commands.feedback._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.services.AuditEventIndexService

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/new"))
class AddAssignment extends CourseworkController {

	@Autowired var assignmentService: AssignmentService = _

	@ModelAttribute("academicYearChoices") def academicYearChoices: java.util.List[AcademicYear] = {
		AcademicYear.guessByDate(DateTime.now).yearsSurrounding(2, 2)
	}

	validatesWith { (cmd: AddAssignmentCommand, errors) =>
		cmd.validate(errors)
	}

	@ModelAttribute def addAssignmentForm(@PathVariable module: Module) =
		new AddAssignmentCommand(mandatory(module))

	// Used for initial load and for prefilling from a chosen assignment
	@RequestMapping()
	def form(user: CurrentUser, @PathVariable module: Module,
		form: AddAssignmentCommand, errors: Errors) = {
		permCheck(module)
		form.afterBind()
		form.prefillFromRecentAssignment()
		formView(form, module)
	}

	// when reloading the form
	@RequestMapping(params = Array("action=refresh"))
	def formRefresh(user: CurrentUser, @PathVariable module: Module,
		form: AddAssignmentCommand, errors: Errors) = {
		permCheck(module)
		form.afterBind()
		formView(form, module)
	}

	@RequestMapping(method = Array(POST), params = Array("action=submit"))
	def submit(user: CurrentUser, @PathVariable module: Module,
		@Valid form: AddAssignmentCommand, errors: Errors) = {
		form.afterBind()
		permCheck(module)
		if (errors.hasErrors) {
			formView(form, module)
		} else {
			form.apply
			Redirect(Routes.admin.module(module))
		}
	}

	def permCheck(module: Module) = mustBeAbleTo(Participate(module))

	def formView(form: AddAssignmentCommand, module: Module) = {
		Mav("admin/assignments/new",
			"department" -> module.department,
			"module" -> module,
			"assessmentGroup" -> form.assessmentGroup)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/edit"))
class EditAssignment extends CourseworkController {

	validatesWith { (form: EditAssignmentCommand, errors: Errors) =>
		form.validate(errors)
		if (form.academicYear != form.assignment.academicYear) {
			errors.rejectValue("academicYear", "academicYear.immutable")
		}
	}

	@ModelAttribute def formObject(@PathVariable("assignment") assignment: Assignment) =
		new EditAssignmentCommand(mandatory(assignment))

	@RequestMapping
	def showForm(@PathVariable module: Module, @PathVariable assignment: Assignment,
		form: EditAssignmentCommand, errors: Errors) = {
		mustBeLinked(assignment, module)
		checkPerms(module)
		form.afterBind()

		val couldDelete = canDelete(assignment)
		Mav("admin/assignments/edit",
			"department" -> module.department,
			"module" -> module,
			"assignment" -> assignment,
			"canDelete" -> couldDelete,
			"assessmentGroup" -> form.assessmentGroup)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	@RequestMapping(method = Array(RequestMethod.POST), params = Array("action!=refresh"))
	def submit(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@Valid form: EditAssignmentCommand, errors: Errors) = {
		mustBeLinked(assignment, module)
		checkPerms(module)
		if (errors.hasErrors) {
			showForm(module, assignment, form, errors)
		} else {
			form.afterBind()
			form.apply
			Redirect(Routes.admin.module(module))
		}

	}

	private def checkPerms(module: Module) {
		mustBeAbleTo(Participate(module))
	}

	private def canDelete(assignment: Assignment): Boolean = {
		val cmd = new DeleteAssignmentCommand(assignment)
		val errors = new BeanPropertyBindingResult(cmd, "cmd")
		cmd.prechecks(errors)
		!errors.hasErrors
	}

}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/delete"))
class DeleteAssignment extends CourseworkController {

	validatesWith { (form: DeleteAssignmentCommand, errors: Errors) =>
		form.validate(errors)
	}

	@ModelAttribute def formObject(@PathVariable("assignment") assignment: Assignment) =
		new DeleteAssignmentCommand(mandatory(assignment))

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def showForm(@PathVariable module: Module, @PathVariable assignment: Assignment,
		form: DeleteAssignmentCommand, errors: Errors) = {

		if (assignment.module != module) throw new ItemNotFoundException
		mustBeAbleTo(Participate(module))
		Mav("admin/assignments/delete",
			"department" -> module.department,
			"module" -> module,
			"assignment" -> assignment)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	@RequestMapping(method = Array(RequestMethod.POST))
	def submit(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@Valid form: DeleteAssignmentCommand, errors: Errors) = {

		mustBeAbleTo(Participate(module))
		if (errors.hasErrors) {
			showForm(module, assignment, form, errors)
		} else {
			form.apply
			Redirect(Routes.admin.module(module))
		}

	}

}

