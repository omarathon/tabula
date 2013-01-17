package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.Features
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMethod, RequestMapping, PathVariable}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.actions.Manage
import scala.Array
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.commands.departments.FeedbackReportCommand
import org.apache.poi.xssf.usermodel.{ XSSFSheet, XSSFWorkbook }
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.ss.usermodel.{ IndexedColors, ComparisonOperator }
import org.apache.poi.ss.util.CellRangeAddress
import uk.ac.warwick.tabula.coursework.web.views.ExcelView
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.AuditEventIndexService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.data.model.Submission
import java.util.ArrayList
import org.joda.time.format.DateTimeFormat
import org.apache.poi.xssf.usermodel.XSSFFont
import uk.ac.warwick.tabula.data.model.AuditEvent

@Controller
@RequestMapping(Array("/admin/department/{dept}/reports/feedback"))
class FeedbackReportController extends CourseworkController {
	
	@Autowired var features: Features = _
	@Autowired var assignmentService: AssignmentService = _
	var auditIndexService = Wire.auto[AuditEventIndexService]
	@ModelAttribute def feedbackReportCommand(@PathVariable dept:Department) = new FeedbackReportCommand(dept, features)

	val dateFormatter = DateTimeFormat.forPattern("dd/MM/yyyy")
	
	
	@RequestMapping(method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def viewReport(@PathVariable dept: Department, user: CurrentUser, cmd:FeedbackReportCommand, errors:Errors) = {
		mustBeAbleTo(Manage(cmd.department))
			
		val events = auditIndexService.findPublishFeedbackEvents(dept)	
		
		val workbook = new XSSFWorkbook()
		val sheet = generateNewSheet(dept, workbook) 
		
		for (event <- events) {
			val submissions = getSubmissionsForFeedbackEvent(event)
			
			for (submission <- submissions) {
				val row = sheet.createRow(sheet.getLastRowNum() + 1)
				row.createCell(0).setCellValue(submission.assignment.module.code)				
				row.createCell(1).setCellValue(dateFormatter.print(submission.submittedDate))
				row.createCell(2).setCellValue(dateFormatter.print(event.eventDate))
			}			
		}
		
		formatWorksheet(sheet)
		new ExcelView(dept.name + "-feedback.xlsx", workbook)
	}
	
	
	
	private def getSubmissionsForFeedbackEvent(event: AuditEvent) : Array[Submission] = {
		val assignment = assignmentService.getAssignmentById(event.assignmentId.get)
		val students = event.parsedData.get("students").asInstanceOf[ArrayList[String]].toArray.toList

		val submissions = assignment.get.submissions.toArray().map(_.asInstanceOf[Submission])
		
		// only return the submissions from students in this feedback round and 
		// also anything that was submitted before the feedback was sent out
		submissions.filter(x => students.contains(x.universityId) && x.submittedDate.isBefore(event.eventDate) )
	}
	
	
	
	def generateNewSheet(dept: Department, workbook: XSSFWorkbook) = {
		val sheet = workbook.createSheet("Report for " + safeDeptName(dept))

		// add header row
		val header = sheet.createRow(0)
		header.createCell(0).setCellValue("Module code")
		header.createCell(1).setCellValue("Submission date")
		header.createCell(2).setCellValue("Return date")
		header.createCell(3).setCellValue("Within 20 days?")

		sheet
	}

	def formatWorksheet(sheet: XSSFSheet) = {
	    (0 to 5) map (sheet.autoSizeColumn(_))
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