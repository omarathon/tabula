package uk.ac.warwick.tabula.coursework.commands.departments

import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.data.model.{Assignment, Department}
import uk.ac.warwick.tabula.services.{AuditEventQueryMethods, AssignmentService, AssignmentMembershipService}
import uk.ac.warwick.spring.Wire
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.ss.util.WorkbookUtil
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl
import collection.JavaConversions._
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import scala.reflect.BeanProperty
import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.DateFormats
import org.joda.time.ReadableInstant
//import org.joda.time.format.DateTimeFormat

class FeedbackReportCommand (val department:Department) extends Command[XSSFWorkbook] with ReadOnly with Unaudited {
	
	PermissionCheck(Permissions.Department.DownloadFeedbackReport, department)

	@BeanProperty var startDate:DateTime = _
	@BeanProperty var endDate:DateTime = _

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty
	val defaultStartDate = new DateTime().minusMonths(3)

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty
	val defaultEndDate = new DateTime()
	
	val csvDateFormatter = DateFormats.CSVDate

	def csvFormat(i: ReadableInstant) = csvDateFormatter print i
	
	
	var assignmentService = Wire.auto[AssignmentService]
	var auditEventQueryMethods = Wire.auto[AuditEventQueryMethods]
	var assignmentMembershipService = Wire.auto[AssignmentMembershipService]

	var workingDaysHelper = new WorkingDaysHelperImpl
	
	
	var dateCellStyle : XSSFCellStyle = null
	var percentageCellStyle : XSSFCellStyle = null
	
	
	def applyInternal() = {
		val workbook = new XSSFWorkbook()
		dateCellStyle = createDateCellStyle(workbook)
		percentageCellStyle = createPercentageCellStyle(workbook)
		val assignmentSheet = generateAssignmentSheet(department, workbook)
		populateAssignmentSheet(assignmentSheet)
		formatWorksheet(assignmentSheet)	
		workbook
	}

	def addCell(value: String, row: XSSFRow) : Int = {
		val cellNum = if(row.getLastCellNum == -1) 0 else row.getLastCellNum 	// if row has no cells, getLastCellNum() returns -1. aargh.
		row.createCell(cellNum).setCellValue(value)
		cellNum
	}
	
	
	def addCell(value: String, row: XSSFRow, style: XSSFCellStyle) : Int = {
		var cellNum = addCell(value, row)
		var cell = row.getCell(cellNum)
		cell.setCellStyle(style)
		cellNum
	}

	def createDateCellStyle(workbook: XSSFWorkbook) = {
		val createHelper = workbook.getCreationHelper
		val cellStyle = workbook.createCellStyle
		cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yy"))
		cellStyle
	}
	
	def createPercentageCellStyle(workbook: XSSFWorkbook) = {
		val createHelper = workbook.getCreationHelper
		val cellStyle = workbook.createCellStyle
		cellStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
		cellStyle
	}
	
	
	def generateAssignmentSheet(dept: Department, workbook: XSSFWorkbook) = {		
		val sheet = workbook.createSheet("Report for " + safeDeptName(dept))

		// add header row
		val header = sheet.createRow(0)
		addCell("Assignment name", header)
		addCell("Module code", header)
		addCell("Close date", header)
		addCell("Expected submissions", header)
		addCell("Actual submissions", header)
		addCell("Late submissions - within extension", header)
		addCell("Late submissions - without extension", header)
		addCell("On-time feedback", header)
		addCell("On-time feedback %", header)
		addCell("Late feedback", header)
		addCell("Late feedback %", header)
		
		// set header style
		var headerStyle = workbook.createCellStyle();
		var font = workbook.createFont()
		font.setBoldweight(Font.BOLDWEIGHT_BOLD)
		font.setFontName("Arial")
    	headerStyle.setFont(font)
    	header.setRowStyle(headerStyle)
    	
		sheet
	}

	def populateAssignmentSheet(sheet: XSSFSheet) {
		for (module <- department.modules;
			 assignment <- module.assignments.filter( a => a.collectSubmissions && a.submissions.size > 0 && a.closeDate.isBefore(endDate) && a.closeDate.isAfter(startDate))) {
		
			val row = sheet.createRow(sheet.getLastRowNum + 1)
			addCell(assignment.name, row)
			addCell(assignment.module.code.toUpperCase, row)
			addCell(csvFormat(assignment.closeDate), row, dateCellStyle)
			val numberOfStudents = assignmentMembershipService.determineMembershipUsers(assignment).size
			addCell(numberOfStudents.toString, row)
			addCell(assignment.submissions.size.toString, row)
			addCell(assignment.submissions.filter(submission => submission.isAuthorisedLate).size.toString, row)
			addCell(assignment.submissions.filter(submission => submission.isLate && !submission.isAuthorisedLate).size.toString, row)
			val (onTime, late) = getFeedbackCounts(assignment)
			val totalPublished = onTime + late
			val onTimePercentage = if (totalPublished == 0) "N/A" else ((onTime / totalPublished)*100).toString
			val latePercentage = if (totalPublished == 0) "N/A" else ((late / totalPublished)*100).toString
			addCell(onTime.toString, row)
			addCell(onTimePercentage, row, percentageCellStyle)
			addCell(late.toString, row)
			addCell(latePercentage, row, percentageCellStyle)
		}
	}

	def formatWorksheet(sheet: XSSFSheet) {
		val dimension = sheet.getCTWorksheet.getDimension
		(0 to 11).map(sheet.autoSizeColumn(_))
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
	
	// returns a pair - first is number of on time feedback, second is number of late
	def getFeedbackCounts(assignment: Assignment) : (Int, Int) =  {

		val times:Seq[(Int, Int)] = for (
			student <- assignmentMembershipService.determineMembershipUsers(assignment);
			submissionEvent <- auditEventQueryMethods.submissionForStudent(assignment, student).headOption ;
			publishEvent <- auditEventQueryMethods.publishFeedbackForStudent(assignment, student).headOption;
			submissionEventDate <- Option(submissionEvent.eventDate);
			publishEventDate <- Option(publishEvent.eventDate);
			assignmentCloseDate <- Option(assignment.closeDate)
			if (!(publishEventDate.isBefore(submissionEventDate) || publishEventDate.isBefore(assignmentCloseDate)))
		) yield {
			// was feedback returned within 20 working days?
			val numOfDays = if(submissionEventDate.toLocalDate.isAfter(assignmentCloseDate.toLocalDate)){
				workingDaysHelper.getNumWorkingDays(submissionEventDate.toLocalDate, publishEventDate.toLocalDate)
			} else {
				workingDaysHelper.getNumWorkingDays(assignmentCloseDate.toLocalDate, publishEventDate.toLocalDate)
			}
			if(numOfDays > 20) (0,1) // was late
			else (1,0) // on time
		}
		// merge our list of pairs into a single pair of (on time, late)
		times.foldLeft(0,0)((a,b) => (a._1 + b._1 , a._2 + b._2))
	}
}