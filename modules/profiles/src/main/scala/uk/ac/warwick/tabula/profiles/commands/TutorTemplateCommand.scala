package uk.ac.warwick.tabula.profiles.commands

import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.views.ExcelView

class TutorTemplateCommand(val department: Department) extends Command[ExcelView] with Unaudited {
	
	PermissionCheck(Permissions.Profiles.PersonalTutor.Upload, department)
	
	def applyInternal() = {
		val workbook = new XSSFWorkbook()
		val sheet = generateNewSheet(department, workbook)

		new ExcelView(safeDepartmentName(department) + " tutors.xlsx", workbook)
	}

	def generateNewSheet(department: Department, workbook: XSSFWorkbook) = {
		val sheet = workbook.createSheet("Tutors for " + safeDepartmentName(department))		
		val style = workbook.createCellStyle
		val format = workbook.createDataFormat
		
		// using an @ sets text format (from BuiltinFormats.class)
		style.setDataFormat(format.getFormat("@"))
		
		// set style on all three columns
		0 to 2 foreach {
			col => sheet.setDefaultColumnStyle(col, style)
			sheet.setColumnWidth(col, 3000) // unit = typical_char_width/256
		}

		// add header row
		val header = sheet.createRow(0)

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