package uk.ac.warwick.tabula.coursework.commands.departments
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, Department}
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
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl
import collection.JavaConversions._
import org.apache.poi.xssf.usermodel.XSSFRow
import uk.ac.warwick.tabula.services.AssignmentMembershipService

class FeedbackReportCommand (val department:Department) extends Command[XSSFWorkbook] with ReadOnly with Unaudited { 
	
	PermissionCheck(Permissions.Department.DownloadFeedbackReport, department)

	var assignmentService = Wire.auto[AssignmentService]
	var auditIndexService = Wire.auto[AuditEventIndexService]
	var assignmentMembershipService = Wire.auto[AssignmentMembershipService]

	var workingDaysHelper = new WorkingDaysHelperImpl
	
	val dateFormatter = DateTimeFormat.forPattern("dd/MM/yyyy")
	
	def applyInternal() = {
		val events = auditIndexService.findPublishFeedbackEvents(department)
		
		val workbook = new XSSFWorkbook()
		val sheet = generateAssignmentSheet(department, workbook) 
		
		val populatedSheet = populateAssignmentSheet(sheet)
		/*
		for (event <- events;
			 assignmentId <- event.assignmentId;
			 assignment <- assignmentService.getAssignmentById(assignmentId)) {

				val row = sheet.createRow(sheet.getLastRowNum() + 1)
				row.createCell(0).setCellValue(assignment.module.code.toUpperCase())				
				row.createCell(1).setCellValue(dateFormatter.print(assignment.closeDate))
					
		}
		*/
		
		
		formatWorksheet(sheet)
		workbook
	}
	
	
	
	def populateAssignmentSheet(sheet: XSSFSheet) {
		for (module <- department.modules;
			 assignment <- module.assignments.filter( a => a.collectSubmissions && a.submissions.size > 0)) {
		
			val row = sheet.createRow(sheet.getLastRowNum() + 1)
			addCell(assignment.name, row)
			addCell(assignment.module.code.toUpperCase(), row)
			addCell(dateFormatter.print(assignment.closeDate), row)
			val numberOfStudents = assignmentMembershipService.determineMembershipUsers(assignment).size
			addCell(numberOfStudents.toString, row)
			addCell(assignment.submissions.size.toString, row)
			addCell(assignment.submissions.filter(submission => submission.isAuthorisedLate).size.toString, row)
			addCell(assignment.submissions.filter(submission => submission.isLate && !submission.isAuthorisedLate).size.toString, row)
			val (onTime, late) = getFeedbackCounts(assignment)
			addCell(onTime.toString, row)
			addCell(((onTime / assignment.submissions.size)*100).toString, row)
			addCell(late.toString, row)
			addCell(((late / assignment.submissions.size)*100).toString, row)
		}
	}
	
	def addCell(value: String, row: XSSFRow) {
		val cellNum = if(row.getLastCellNum() == -1) 0 else row.getLastCellNum() 	// if row has no cells, getLastCellNum() returns -1. aargh.
		row.createCell(cellNum).setCellValue(value)
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
	
	
	def generateAssignmentSheet(dept: Department, workbook: XSSFWorkbook) = {
		val sheet = workbook.createSheet("Report for " + safeDeptName(dept))

		// add header row
		val header = sheet.createRow(0)
		header.createCell(0).setCellValue("Assignment name")
		header.createCell(1).setCellValue("Module code")
		header.createCell(2).setCellValue("Close date")
		header.createCell(3).setCellValue("Expected submissions")
		header.createCell(4).setCellValue("Actual submissions")
		header.createCell(5).setCellValue("Late submissions - within extension")
		header.createCell(6).setCellValue("Late submissions - without extension")
		header.createCell(7).setCellValue("On-time feedback")
		header.createCell(8).setCellValue("On-time feedback %")
		header.createCell(9).setCellValue("Late feedback")
		header.createCell(10).setCellValue("Late feedback %")
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
	
	
	// returns a pair - first is number of on time feedbacks, second is number of late
	def getFeedbackCounts(assignment: Assignment) : (Int, Int) =  {
		//val submissionEvents = auditIndexService.submissionsForAssignment(assignment)
		var onTime, late = 0
		val students = assignmentMembershipService.determineMembershipUsers(assignment)
		
		
		students.foreach { student => 
			val submissionEvents = auditIndexService.submissionForStudent(assignment, student)
			submissionEvents.headOption.foreach { submissionEvent =>
				
				val publishEvents = auditIndexService.publishFeedbackForStudent(assignment, student)
				publishEvents.headOption.foreach { publishEvent =>
				
					val submissionEventDate = submissionEvent.eventDate.toLocalDate
					val publishEventDate = publishEvent.eventDate.toLocalDate
					val assignmentCloseDate = assignment.closeDate.toLocalDate
					
					if(publishEventDate.isAfter(submissionEventDate) && publishEventDate.isAfter(assignmentCloseDate)) {
						// was feedback returned within 20 working days?
						val numOfDays = if(submissionEvent.eventDate.isAfter(assignment.closeDate)){
							workingDaysHelper.getNumWorkingDays(submissionEvent.eventDate.toLocalDate, publishEvent.eventDate.toLocalDate)
						} else {
							workingDaysHelper.getNumWorkingDays(assignment.closeDate.toLocalDate, publishEvent.eventDate.toLocalDate)
						}
					
						if(numOfDays > 20) {
							late = late + 1
						} else {
							onTime = onTime + 1
						}
					}
				}
			}			
		}
		
		(onTime, late)
	}
	
	
	
}