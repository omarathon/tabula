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
	
	var assignmentData : List[AssignmentInfo] = List()
	var moduleData: List[ModuleInfo] = List()
	
	def applyInternal() = {
		val workbook = new XSSFWorkbook()
		dateCellStyle = createDateCellStyle(workbook)
		percentageCellStyle = createPercentageCellStyle(workbook)
		buildAssignmentData
		
		val assignmentSheet = generateAssignmentSheet(department, workbook)
		val moduleSheet = generateModuleSheet(department, workbook)

		populateAssignmentSheet(assignmentSheet)
		populateModuleSheet(moduleSheet)
		
		formatWorksheet(assignmentSheet)
		formatWorksheet(moduleSheet)
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
		val sheet = workbook.createSheet("Assignment report for " + safeDeptName(dept))

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
		for (assignmentInfo <- assignmentData) {
				
			val assignment = assignmentInfo.assignment
			
			val row = sheet.createRow(sheet.getLastRowNum + 1)
			addCell(assignment.name, row)
			addCell(assignment.module.code.toUpperCase, row)
			addCell(csvFormat(assignment.closeDate), row, dateCellStyle)
			addCell(assignmentInfo.membership.toString, row)
			addCell(assignmentInfo.numberOfSubmissions.toString, row)
			addCell(assignmentInfo.submissionsLateWithExt.toString, row)
			addCell(assignmentInfo.submissionsLateWithoutExt.toString, row)
			
			val onTimePercentage = calcTimelinessPercentage(assignmentInfo.onTimeFeedback, assignmentInfo.totalPublished)
			val latePercentage = calcTimelinessPercentage(assignmentInfo.lateFeedback, assignmentInfo.totalPublished)
			
			addCell(assignmentInfo.onTimeFeedback.toString, row)
			addCell(onTimePercentage.toString, row, percentageCellStyle)
			addCell(assignmentInfo.lateFeedback.toString, row)
			addCell(latePercentage.toString, row, percentageCellStyle)
		}
	}
	
	def calcTimelinessPercentage(number: Int, totalPublished: Int) = {
		if (totalPublished == 0) "N/A" else ((number / totalPublished)*100)
	}
	
	
	
	def buildAssignmentData {
		for (module <- department.modules;
			 assignment <- module.assignments.filter( a => a.collectSubmissions && a.submissions.size > 0 
					 								   && a.closeDate.isAfter(startDate) && a.closeDate.isBefore(endDate))) {
			
			var assignmentInfo = new AssignmentInfo
			assignmentInfo.numberOfSubmissions = assignment.submissions.size
			assignmentInfo.membership =  assignmentMembershipService.determineMembershipUsers(assignment).size
			assignmentInfo.submissionsLateWithExt = assignment.submissions.filter(submission => submission.isAuthorisedLate).size
			assignmentInfo.submissionsLateWithoutExt = assignment.submissions.filter(submission => submission.isLate && !submission.isAuthorisedLate).size
			val (onTime, late) = getFeedbackCounts(assignment)
			assignmentInfo.onTimeFeedback = onTime
			assignmentInfo.lateFeedback = late
			assignmentInfo.totalPublished = onTime + late
			assignmentInfo.moduleCode = assignment.module.code
			assignmentInfo.moduleName = assignment.module.name
			assignmentInfo.assignment = assignment
			
			assignmentData = assignmentData ++ List(assignmentInfo)
		}
	}
	


	
	def generateModuleSheet(dept: Department, workbook: XSSFWorkbook) = {		
		val sheet = workbook.createSheet("Module report for " + safeDeptName(dept))
		
		// add header row
		val header = sheet.createRow(0)
		addCell("Module name", header)
		addCell("Module code", header)
		addCell("Number of assignments", header)
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
	
	
	def populateModuleSheet(sheet: XSSFSheet) {	
		val modules = assignmentData.groupBy(_.assignment.module.code)
		for (module <- modules) {
			val row = sheet.createRow(sheet.getLastRowNum + 1)
			val moduleCode = module._1
			val assignmentInfoList= module._2
			
			addCell(assignmentInfoList(0).moduleName, row)
			addCell(moduleCode, row)
			addCell(assignmentInfoList.groupBy(_.assignment.name).size.toString, row)

			val expectedSubmissions = assignmentInfoList.map(a => a.membership).sum			
			addCell(expectedSubmissions.toString, row)
			val numberOfSubmissions = assignmentInfoList.map(a => a.numberOfSubmissions).sum			
			addCell(numberOfSubmissions.toString, row)
			val submissionsLateWithExt = assignmentInfoList.map(a => a.submissionsLateWithExt).sum
			addCell(submissionsLateWithExt.toString, row)
			val submissionsLateWithoutExt = assignmentInfoList.map(a => a.submissionsLateWithoutExt).sum
			addCell(submissionsLateWithoutExt.toString, row)
			val totalPublished = assignmentInfoList.map(a => a.totalPublished).sum
			val ontime = assignmentInfoList.map(a => a.onTimeFeedback).sum
			addCell(ontime.toString, row)
			addCell(calcTimelinessPercentage(ontime, totalPublished).toString, row)
			val late = assignmentInfoList.map(a => a.lateFeedback).sum
			addCell(late.toString, row)
			addCell(calcTimelinessPercentage(late, totalPublished).toString, row)	
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

class AssignmentInfo {
	var moduleCode: String = _
	var moduleName: String = _
	var membership: Int = _
	var numberOfSubmissions: Int = _
	var submissionsLateWithExt: Int = _ 
	var submissionsLateWithoutExt: Int = _
	var onTimeFeedback: Int = _
	var lateFeedback: Int = _
	var totalPublished: Int = _
	var assignment: Assignment = _
}

class ModuleInfo {
	var moduleCode: String = _
	var moduleName: String = _
	var numberOfSubmissions: Int = _
	var submissionsLateWithExt: Int = _ 
	var submissionsLateWithoutExt: Int = _
	var onTimeFeedback: Int = _
	var lateFeedback: Int = _
	var totalPublished: Int = _
}

