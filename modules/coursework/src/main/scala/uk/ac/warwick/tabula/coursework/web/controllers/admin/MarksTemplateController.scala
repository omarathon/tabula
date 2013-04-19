package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.apache.poi.ss.usermodel.{IndexedColors, ComparisonOperator}
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.web.views.ExcelView
import uk.ac.warwick.tabula.services.FeedbackService

class GenerateMarksTemplateCommand(val module: Module, val assignment: Assignment) extends Command[XSSFWorkbook] with ReadOnly with Unaudited {
	import MarksTemplateCommand._
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Marks.DownloadTemplate, assignment)

	var members:Seq[String] =_
	var feedbackService = Wire[FeedbackService]
	var assignmentMembershipService = Wire[AssignmentMembershipService]

	def applyInternal() = {

		val workbook = new XSSFWorkbook()
		val sheet = generateNewMarkSheet(assignment, workbook)

		// populate the mark sheet with ids
		for ((member, i) <- members.zipWithIndex) {
			val row = sheet.createRow(i + 1)
			row.createCell(0).setCellValue(member)
			val marksCell = row.createCell(1)
			val gradesCell = row.createCell(2)
			val feedbacks = feedbackService.getStudentFeedback(assignment, member)
			feedbacks.foreach { feedback =>
			  feedback.actualMark.foreach(marksCell.setCellValue(_))
			  feedback.actualGrade.foreach(gradesCell.setCellValue(_))
			}
		}

		// add conditional formatting for invalid marks
		addConditionalFormatting(sheet)
		
		workbook
	}

	def generateNewMarkSheet(assignment: Assignment, workbook: XSSFWorkbook) = {
		val sheet = workbook.createSheet("Marks for " + safeAssignmentName(assignment))

		// add header row
		val header = sheet.createRow(0)
		header.createCell(0).setCellValue("ID")
		header.createCell(1).setCellValue("Mark")
		header.createCell(2).setCellValue("Grade")

		sheet
	}

	def addConditionalFormatting(sheet: XSSFSheet) = {
		val sheetCF = sheet.getSheetConditionalFormatting

		val invalidMarkRule = sheetCF.createConditionalFormattingRule(ComparisonOperator.NOT_BETWEEN, "0", "100")
		val fontFmt = invalidMarkRule.createFontFormatting
		fontFmt.setFontStyle(true, false)
		fontFmt.setFontColorIndex(IndexedColors.DARK_RED.index)

		val marksColumn = Array(new CellRangeAddress(1, sheet.getLastRowNum, 1, 1))
		sheetCF.addConditionalFormatting(marksColumn, invalidMarkRule)
	}
	
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marks-template"))
class MarksTemplateController extends CourseworkController {
	import MarksTemplateCommand._

	var assignmentMembershipService = Wire[AssignmentMembershipService]
	
	@ModelAttribute def command(@PathVariable("module") module: Module, @PathVariable(value = "assignment") assignment: Assignment) =
		new GenerateMarksTemplateCommand(module, assignment)

	@RequestMapping(method = Array(HEAD, GET))
	def generateMarksTemplate(cmd: GenerateMarksTemplateCommand) = {
		cmd.members = assignmentMembershipService.determineMembershipUsers(cmd.assignment).map(_.getWarwickId)
		new ExcelView(safeAssignmentName(cmd.assignment) + " marks.xlsx", cmd.apply())
	}
}


@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/marks-template"))
class MarkerMarksTemplateController extends CourseworkController {
	import MarksTemplateCommand._

	var assignmentService = Wire[AssignmentService]

	@ModelAttribute def command(@PathVariable module: Module, @PathVariable(value = "assignment") assignment: Assignment) =
		new GenerateMarksTemplateCommand(module, assignment)

	@RequestMapping(method = Array(HEAD, GET))
	def generateMarksTemplate(cmd: GenerateMarksTemplateCommand, currentUser: CurrentUser) = {
		cmd.members = cmd.assignment.getMarkersSubmissions(currentUser.apparentUser).map(_.universityId)
		new ExcelView(safeAssignmentName(cmd.assignment) + " marks.xlsx", cmd.apply())
	}
}

object MarksTemplateCommand {

	// util to replace unsafe characters with spaces
	def safeAssignmentName(assignment: Assignment) = WorkbookUtil.createSafeSheetName(trimmedAssignmentName(assignment))

	// trim the assignment name down to 21 characters. Excel sheet names must be 31 chars or less so
	// "Marks for " = 10 chars + assignment name (max 21) = 31
	def trimmedAssignmentName(assignment: Assignment) = {
		if (assignment.name.length > 21)
			assignment.name.substring(0, 21)
		else
			assignment.name
	}	
	
}
