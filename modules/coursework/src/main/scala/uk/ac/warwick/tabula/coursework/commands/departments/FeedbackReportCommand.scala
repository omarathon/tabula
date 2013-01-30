package uk.ac.warwick.tabula.coursework.commands.departments
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.services.AuditEventIndexService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.spring.Wire
import org.joda.time.format.DateTimeFormat
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.ss.util.WorkbookUtil
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.ReadOnly


class FeedbackReportCommand (val department:Department) extends Command[XSSFWorkbook] with ReadOnly with Unaudited { 
	
	PermissionCheck(Permission.Department.DownloadFeedbackReport(), department)

	var assignmentService = Wire.auto[AssignmentService]
	var auditIndexService = Wire.auto[AuditEventIndexService]
	
	val dateFormatter = DateTimeFormat.forPattern("dd/MM/yyyy")
	
	def applyInternal() = {
		val events = auditIndexService.findPublishFeedbackEvents(department)	
		
		val workbook = new XSSFWorkbook()
		val sheet = generateNewSheet(department, workbook) 
		
		for (event <- events;
			 assignmentId <- event.assignmentId;
			 assignment <- assignmentService.getAssignmentById(assignmentId)) {

				val row = sheet.createRow(sheet.getLastRowNum() + 1)
				row.createCell(0).setCellValue(assignment.module.code.toUpperCase())				
				row.createCell(1).setCellValue(dateFormatter.print(assignment.closeDate))
				row.createCell(2).setCellValue(dateFormatter.print(event.eventDate))	
		}
		
		formatWorksheet(sheet)
		workbook
	}
	
	
	def generateNewSheet(dept: Department, workbook: XSSFWorkbook) = {
		val sheet = workbook.createSheet("Report for " + safeDeptName(dept))

		// add header row
		val header = sheet.createRow(0)
		header.createCell(0).setCellValue("Module code")
		header.createCell(1).setCellValue("Close date")
		header.createCell(2).setCellValue("Return date")
		header.createCell(3).setCellValue("Did module meet required 20 University working days turnaround? (Y/N)")
		header.createCell(4).setCellValue("Exemption sought?")
		header.createCell(5).setCellValue("Notes")
		sheet
	}
	
	
	def formatWorksheet(sheet: XSSFSheet) = {
	    (0 to 5) map (sheet.autoSizeColumn(_))
	    sheet.setColumnWidth(5, 40)
	}
	
	
	// trim the department name down to 20 characters. Excel sheet names must be 31 chars or less so
	def trimmedDeptName(dept: Department) = {
		if (dept.name.length > 20)
			dept.name.substring(0, 20)
		else
			dept.name
	}

	// util to replace unsafe characters with spaces
	def safeDeptName(dept: Department) = WorkbookUtil.createSafeSheetName(trimmedDeptName(dept))
	
}