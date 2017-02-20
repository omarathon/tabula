package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.{ModifyAssignmentStudentsCommand, _}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.{Breadcrumbs, Mav}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/new/{assignment}/students"))
class EditAssignmentStudentsController extends CourseworkController {

	type ModifyAssignmentStudentsCommand = ModifyAssignmentStudentsCommandInternal with Appliable[Assignment] with AssignmentStudentsCommandState with ModifiesAssignmentMembership
		with PopulateAssignmentStudentCommand

	validatesSelf[SelfValidating]


	@ModelAttribute("ManageAssignmentMappingParameters")
	def params = ManageAssignmentMappingParameters


	@ModelAttribute("command") def command(@PathVariable assignment: Assignment): ModifyAssignmentStudentsCommand =
		ModifyAssignmentStudentsCommand(mandatory(assignment))


	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("assignment") assignment: Assignment,
		@ModelAttribute("command") cmd: ModifyAssignmentStudentsCommand): Mav = {
		cmd.afterBind()
		cmd.populateGroups(assignment)
		showForm(cmd)
	}


	def showForm(form: ModifyAssignmentStudentsCommand): Mav = {
		val module = form.module
		Mav(s"$urlPrefix/admin/assignments/new_assignment_student_details",
			"department" -> module.adminDepartment,
			"module" -> module,
			"linkedUpstreamAssessmentGroups" -> form.linkedUpstreamAssessmentGroups,
			"availableUpstreamGroups" -> form.availableUpstreamGroups,
			"assessmentGroups" -> form.assessmentGroups,
			"academicYear" -> form.assignment.academicYear
		).secondCrumbs(Breadcrumbs.Standard("Assignment Management", Some(Routes.admin.assignment.createAddStudents(form.assignment)), ""))
	}

	// TODO - add method for save and exit
	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddMarkers, "action!=refresh", "action!=update"))
	def submitAndAddFeedback(@Valid @ModelAttribute("command") cmd: ModifyAssignmentStudentsCommand, errors: Errors): Mav =
	submit(cmd, errors, Routes.admin.assignment.createAddMarkers)


	private def submit(cmd: ModifyAssignmentStudentsCommand, errors: Errors, route: Assignment => String) = {
		cmd.afterBind()
		if (errors.hasErrors) showForm(cmd)
		else {
			val assignment = cmd.apply()
			RedirectForce(route(assignment))
		}
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}
}