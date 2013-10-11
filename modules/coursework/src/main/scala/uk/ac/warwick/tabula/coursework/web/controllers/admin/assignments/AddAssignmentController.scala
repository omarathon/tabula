package uk.ac.warwick.tabula.coursework.web.controllers.admin.assignments

import javax.validation.Valid
import scala.collection.JavaConverters._

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.WebDataBinder
import org.joda.time.DateTime

import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.UpstreamGroup
import uk.ac.warwick.tabula.commands.UpstreamGroupPropertyEditor

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/new"))
class AddAssignmentController extends CourseworkController {

	@Autowired var assignmentService: AssignmentService = _

	@ModelAttribute("academicYearChoices") def academicYearChoices: JList[AcademicYear] = {
		AcademicYear.guessByDate(DateTime.now).yearsSurrounding(2, 2).asJava
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
	
	@RequestMapping(method = Array(POST), params = Array("action=refresh"))
	def submit(form: AddAssignmentCommand) = {
		// No validation here
		form.afterBind()
		showForm(form)
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
			"collectSubmissions" -> form.collectSubmissions,
			"maxWordCount" -> Assignment.MaximumWordCount)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}
}
