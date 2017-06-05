package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.feedback.{GenerateMarksTemplateCommand, GenerateOwnMarksTemplateCommand, MarksTemplateCommand}
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.views.ExcelView
import uk.ac.warwick.userlookup.User

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/marks-template"))
class AssignmentMarksTemplateController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment) = GenerateMarksTemplateCommand(mandatory(assignment))

	@RequestMapping
	def generateMarksTemplate(@ModelAttribute("command") cmd: Appliable[SXSSFWorkbook], @PathVariable assignment: Assignment): ExcelView = {
		new ExcelView(MarksTemplateCommand.safeAssessmentName(assignment) + " marks.xlsx", cmd.apply())
	}
}


@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/{marker}/marks-template"))
class AssignmentMarkerMarksTemplateController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment, @PathVariable marker: User) =
		GenerateOwnMarksTemplateCommand(mandatory(assignment), mandatory(marker))

	@RequestMapping
	def generateMarksTemplate(@ModelAttribute("command") cmd: Appliable[SXSSFWorkbook], @PathVariable assignment: Assignment): ExcelView = {
		new ExcelView(MarksTemplateCommand.safeAssessmentName(assignment) + " marks.xlsx", cmd.apply())
	}
}


@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/marks-template"))
class AssignmentCurrentMarkerMarksTemplateController extends CourseworkController {

	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.marksTemplate(assignment, currentUser.apparentUser))
	}
}
