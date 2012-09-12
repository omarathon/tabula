package uk.ac.warwick.courses.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.courses.data.model.{Module, Assignment}
import uk.ac.warwick.courses.actions.Participate
import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.AssignmentService
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.ss.usermodel.{IndexedColors, ComparisonOperator}
import org.apache.poi.ss.util.CellRangeAddress
import uk.ac.warwick.courses.web.views.ExcelView


@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/marks-template"))
class MarksTemplateController extends BaseController {

	@Autowired var assignmentService:AssignmentService = _

	@RequestMapping(method=Array(HEAD,GET))
	def generateMarksTemplate(@PathVariable module:Module, @PathVariable(value="assignment") assignment:Assignment) = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))

		val members = assignmentService.determineMembershipUsers(assignment)

		val workbook = new XSSFWorkbook()
		val sheet = generateNewMarkSheet(assignment,workbook)

		// populate the mark sheet with ids
		for (member <- members.zipWithIndex){
			val row = sheet.createRow(member._2+1)
			row.createCell(0).setCellValue(member._1.getWarwickId)
			val marksCell = row.createCell(1)
			val feedback = assignmentService.getStudentFeedback( assignment, member._1.getWarwickId)
			feedback.foreach {
				_.actualMark.foreach(marksCell.setCellValue(_))
			}
		}

		// add conditional formatting for invalid marks
		addConditionalFormatting(sheet)

		Mav(new ExcelView(safeAssignmentName(assignment)+" marks.xlsx", workbook))
	}

	def generateNewMarkSheet(assignment:Assignment, workbook:XSSFWorkbook) = {
		val sheet = workbook.createSheet("Marks for "+safeAssignmentName(assignment))

		// add header row
		val header = sheet.createRow(0)
		header.createCell(0).setCellValue("ID")
		header.createCell(1).setCellValue("Mark")

		sheet
	}

	def addConditionalFormatting(sheet:XSSFSheet) = {
		val sheetCF = sheet.getSheetConditionalFormatting

		val invalidMarkRule = sheetCF.createConditionalFormattingRule(ComparisonOperator.NOT_BETWEEN, "0", "100")
		val fontFmt = invalidMarkRule.createFontFormatting
		fontFmt.setFontStyle(true, false)
		fontFmt.setFontColorIndex(IndexedColors.DARK_RED.index)

		val marksColumn = Array(new CellRangeAddress(1,sheet.getLastRowNum,1,1))
		sheetCF.addConditionalFormatting(marksColumn, invalidMarkRule)
	}

	// trim the assignment name down to 21 characters. Excel sheet names must be 31 chars or less so
	// "Marks for " = 10 chars + assignment name (max 21) = 31
	def trimmedAssignmentName(assignment:Assignment) = {
		if (assignment.name.length > 21)
			assignment.name.substring(0,21)
		else
			assignment.name
	}

	// util to replace unsafe characters with spaces
	def safeAssignmentName(assignment:Assignment) = WorkbookUtil.createSafeSheetName(trimmedAssignmentName(assignment))

}
