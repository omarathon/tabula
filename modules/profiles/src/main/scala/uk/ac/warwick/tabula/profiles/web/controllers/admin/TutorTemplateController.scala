package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ PathVariable, RequestMapping }
import uk.ac.warwick.tabula.data.model.{ Module, Assignment }
import uk.ac.warwick.tabula.actions.Participate
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.AssignmentService
import org.apache.poi.xssf.usermodel.{ XSSFSheet, XSSFWorkbook, XSSFCell }
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.ss.usermodel.{ IndexedColors, ComparisonOperator }
import org.apache.poi.ss.util.CellRangeAddress
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.actions.Manage
import uk.ac.warwick.tabula.web.views.ExcelView

@Controller
@RequestMapping(value = Array("/admin/department/{department}/tutors/template"))
class TutorTemplateController extends ProfilesController {

	@RequestMapping(method = Array(HEAD, GET))
	def generateTemplate(@PathVariable department: Department) = {
		mustBeAbleTo(Manage(department))

		val workbook = new XSSFWorkbook()
		val sheet = generateNewSheet(department, workbook)

//		// populate the mark sheet with ids
//		for ((member, i) <- members.zipWithIndex) {
//			val row = sheet.createRow(i + 1)
//			row.createCell(0).setCellValue(member.getWarwickId)
//			val marksCell = row.createCell(1)
//			val gradesCell = row.createCell(2)
//			val feedbacks = assignmentService.getStudentFeedback(assignment, member.getWarwickId)
//			feedbacks.foreach { feedback =>
//			  feedback.actualMark.foreach(marksCell.setCellValue(_))
//			  feedback.actualGrade.foreach(gradesCell.setCellValue(_))
//			}
//		}

		new ExcelView(safeDepartmentName(department) + " tutors.xlsx", workbook)
	}

	def generateNewSheet(department: Department, workbook: XSSFWorkbook) = {
		val sheet = workbook.createSheet("Tutors for " + safeDepartmentName(department))		
		val style = workbook.createCellStyle
		val format = workbook.createDataFormat
		
		// using an @ sets text format (from BuiltinFormats.class)
		style.setDataFormat(format.getFormat("@"))
		
		// set style on all three columns
		1 to 3 foreach { col => sheet.setDefaultColumnStyle(col, style) }

		// add header row
		val header = sheet.createRow(0)

		// tried this but it was a bit hopeful:
		header.createCell(0).setCellValue("student_id").formatted("text")
				
		header.createCell(0).setCellValue("student_id")
		header.createCell(1).setCellValue("tutor_id")
		header.createCell(2).setCellValue("tutor_name")

		sheet
	}

	// trim the assignment name down to 21 characters. Excel sheet names must be 31 chars or less so
	// "Marks for " = 10 chars + assignment name (max 21) = 31
	def trimmedDepartmentName(department: Department) = {
		if (department.name.length > 21)
			department.name.substring(0, 21)
		else
			department.name
	}

	// util to replace unsafe characters with spaces
	def safeDepartmentName(department: Department) = WorkbookUtil.createSafeSheetName(trimmedDepartmentName(department))

}
