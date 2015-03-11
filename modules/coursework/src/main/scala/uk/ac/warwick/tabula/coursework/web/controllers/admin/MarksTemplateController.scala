package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.coursework.commands.feedback.MarksTemplateCommand._
import uk.ac.warwick.tabula.coursework.commands.feedback.{GenerateMarksTemplateCommand, GenerateOwnMarksTemplateCommand}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.web.views.ExcelView
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marks-template"))
class AssignmentMarksTemplateController extends CourseworkController {

	var assignmentMembershipService = Wire[AssessmentMembershipService]
	
	@ModelAttribute("command")
	def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) =
		GenerateMarksTemplateCommand(
			mandatory(module),
			mandatory(assignment),
			assignmentMembershipService.determineMembershipUsers(assignment).map(_.getWarwickId)
		)

	@RequestMapping(method = Array(HEAD, GET))
	def generateMarksTemplate(@ModelAttribute("command") cmd: Appliable[XSSFWorkbook], @PathVariable("assignment") assignment: Assignment) = {
		new ExcelView(safeAssessmentName(assignment) + " marks.xlsx", cmd.apply())
	}
}


@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/{marker}/marks-template"))
class AssignmentMarkerMarksTemplateController extends CourseworkController {

	@ModelAttribute("command")
	def command(
		@PathVariable("module") module: Module,
		@PathVariable("assignment") assignment: Assignment,
		@PathVariable("marker") marker: User
	) =
		GenerateOwnMarksTemplateCommand(
			mandatory(module),
			mandatory(assignment),
			assignment.getMarkersSubmissions(mandatory(marker)).map(_.universityId)
		)

	@RequestMapping(method = Array(HEAD, GET))
	def generateMarksTemplate(@ModelAttribute("command") cmd: Appliable[XSSFWorkbook], @PathVariable("assignment") assignment: Assignment) = {
		new ExcelView(safeAssessmentName(assignment) + " marks.xlsx", cmd.apply())
	}
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/marks-template"))
class CurrentAssignmentMarkerMarksTemplateController extends CourseworkController {

	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser) = {
		Redirect(Routes.admin.assignment.markerFeedback.marksTemplate(assignment, currentUser.apparentUser))
	}
}
