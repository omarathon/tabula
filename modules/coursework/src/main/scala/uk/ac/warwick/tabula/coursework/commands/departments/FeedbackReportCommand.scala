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
import org.apache.poi.ss.usermodel.Cell
import java.util.Date

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

	var assignmentService = Wire.auto[AssignmentService]
	var auditEventQueryMethods = Wire.auto[AuditEventQueryMethods]
	var assignmentMembershipService = Wire.auto[AssignmentMembershipService]

	var workingDaysHelper = new WorkingDaysHelperImpl

	val workbook = new XSSFWorkbook()

	val dateCellStyle : XSSFCellStyle = {
		val createHelper = workbook.getCreationHelper
		val cellStyle = workbook.createCellStyle
		cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yy"))
		cellStyle
	}

	val percentageCellStyle : XSSFCellStyle = {
		val cellStyle = workbook.createCellStyle
		cellStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"))
		cellStyle
	}

	val headerStyle : XSSFCellStyle = {
		val cellStyle = workbook.createCellStyle
		val font = workbook.createFont()
		font.setBoldweight(Font.BOLDWEIGHT_BOLD)
		cellStyle.setFont(font)
		cellStyle
	}

	// trim the department name down to 20 characters. Excel sheet names must be 31 chars or less so
	lazy val trimmedDeptName = {
		if (department.name.length > 20)
			department.name.substring(0, 20)
		else
			department.name
	}

	// replace unsafe characters with spaces
	lazy val safeDeptName = WorkbookUtil.createSafeSheetName(trimmedDeptName)

	var assignmentData : List[AssignmentInfo] = List()

	def applyInternal() = {
		val assignmentSheet = generateAssignmentSheet(department)
		val moduleSheet = generateModuleSheet(department, workbook)

		buildAssignmentData()

		populateAssignmentSheet(assignmentSheet)
		formatWorksheet(assignmentSheet)
		populateModuleSheet(moduleSheet)
		
		formatWorksheet(assignmentSheet)
		formatWorksheet(moduleSheet)
		workbook
	}

	def getNextCellNum(row: XSSFRow):Short = if(row.getLastCellNum == -1) 0 else row.getLastCellNum

	def addCell(row: XSSFRow, cellType: Int) = {
		val cell = row.createCell(getNextCellNum(row))
		cell.setCellType(cellType)
		cell
	}

	def addStringCell(value: String, row: XSSFRow) {
		val cell = addCell(row, Cell.CELL_TYPE_STRING)
		cell.setCellValue(value)
	}

	def addStringCell(value: String, row: XSSFRow, style: XSSFCellStyle) {
		val cell = addCell(row, Cell.CELL_TYPE_STRING)
		cell.setCellStyle(style)
		cell.setCellValue(value)
	}

	def addNumericCell(value: Double, row: XSSFRow) {
		val cell = addCell(row, Cell.CELL_TYPE_NUMERIC)
		cell.setCellValue(value)
	}
	
	def addNumericCell(value: Double, row: XSSFRow, style: XSSFCellStyle) {
		val cell = addCell(row, Cell.CELL_TYPE_NUMERIC)
		cell.setCellStyle(style)
		cell.setCellValue(value)
	}

	def addDateCell(value: Date, row: XSSFRow, style: XSSFCellStyle) {
		val cell = addCell(row, Cell.CELL_TYPE_NUMERIC)
		cell.setCellStyle(style)
		cell.setCellValue(value)
	}

	def addPercentageCell(num:Double, total:Double, row: XSSFRow) {
		if (total == 0)
			addStringCell("N/A", row)
		else
			addNumericCell((num / total), row, percentageCellStyle)
	}

	def formatWorksheet(sheet: XSSFSheet) {
		(0 to 11).map(sheet.autoSizeColumn(_))
	}

	def generateAssignmentSheet(dept: Department) = {
		val sheet = workbook.createSheet("Report for " + safeDeptName)

		// add header row
		val header = sheet.createRow(0)

		addStringCell("Assignment name", header, headerStyle)
		addStringCell("Module code", header, headerStyle)
		addStringCell("Close date", header, headerStyle)
		addStringCell("Expected submissions", header, headerStyle)
		addStringCell("Actual submissions", header, headerStyle)
		addStringCell("Late submissions - within extension", header, headerStyle)
		addStringCell("Late submissions - without extension", header, headerStyle)
		addStringCell("On-time feedback", header, headerStyle)
		addStringCell("On-time feedback %", header, headerStyle)
		addStringCell("Late feedback", header, headerStyle)
		addStringCell("Late feedback %", header, headerStyle)

		sheet
	}

	def populateAssignmentSheet(sheet: XSSFSheet) {
		for (assignmentInfo <- assignmentData) {
				
			val assignment = assignmentInfo.assignment
			
			val row = sheet.createRow(sheet.getLastRowNum + 1)
			addStringCell(assignment.name, row)
			addStringCell(assignment.module.code.toUpperCase, row)
			addDateCell(assignment.closeDate.toDate, row, dateCellStyle)
			val numberOfStudents = assignmentMembershipService.determineMembershipUsers(assignment).size
			addNumericCell(numberOfStudents, row)
			addNumericCell(assignment.submissions.size, row)
			addNumericCell(assignment.submissions.filter(submission => submission.isAuthorisedLate).size, row)
			addNumericCell(assignment.submissions.filter(submission => submission.isLate && !submission.isAuthorisedLate).size, row)
			val (onTime, late) = getFeedbackCounts(assignment)
			val totalPublished = onTime + late
			addNumericCell(onTime, row)
			addPercentageCell(onTime, totalPublished, row)
			addNumericCell(late, row)
			addPercentageCell(late, totalPublished, row)
		}
	}
	
	def buildAssignmentData() {
		for (module <- department.modules;
				assignment <- module.assignments.filter( a => a.collectSubmissions && a.submissions.size > 0
				&& a.closeDate.isAfter(startDate) && a.closeDate.isBefore(endDate))) {

			val assignmentInfo = new AssignmentInfo
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
		val sheet = workbook.createSheet("Module report for " + safeDeptName)
		
		// add header row
		val header = sheet.createRow(0)
		addStringCell("Module name", header, headerStyle)
		addStringCell("Module code", header, headerStyle)
		addStringCell("Number of assignments", header, headerStyle)
		addStringCell("Expected submissions", header, headerStyle)
		addStringCell("Actual submissions", header, headerStyle)
		addStringCell("Late submissions - within extension", header, headerStyle)
		addStringCell("Late submissions - without extension", header, headerStyle)
		addStringCell("On-time feedback", header, headerStyle)
		addStringCell("On-time feedback %", header, headerStyle)
		addStringCell("Late feedback", header, headerStyle)
		addStringCell("Late feedback %", header, headerStyle)

		sheet
	}


	def populateModuleSheet(sheet: XSSFSheet) {	
		val modules = assignmentData.groupBy(_.assignment.module.code)
		for (module <- modules) {
			val row = sheet.createRow(sheet.getLastRowNum + 1)
			val moduleCode = module._1
			val assignmentInfoList= module._2

			addStringCell(assignmentInfoList(0).moduleName, row)
			addStringCell(moduleCode, row)
			addNumericCell(assignmentInfoList.groupBy(_.assignment.name).size, row)

			val expectedSubmissions = assignmentInfoList.map(_.membership).sum
			addNumericCell(expectedSubmissions, row)
			val numberOfSubmissions = assignmentInfoList.map(_.numberOfSubmissions).sum
			addNumericCell(numberOfSubmissions, row)
			val submissionsLateWithExt = assignmentInfoList.map(_.submissionsLateWithExt).sum
			addNumericCell(submissionsLateWithExt, row)
			val submissionsLateWithoutExt = assignmentInfoList.map(_.submissionsLateWithoutExt).sum
			addNumericCell(submissionsLateWithoutExt, row)
			val totalPublished = assignmentInfoList.map(_.totalPublished).sum
			val ontime = assignmentInfoList.map(_.onTimeFeedback).sum
			addNumericCell(ontime, row)
			addPercentageCell(ontime, totalPublished, row)
			val late = assignmentInfoList.map(_.lateFeedback).sum
			addNumericCell(late, row)
			addPercentageCell(late, totalPublished, row)
		}
	}
	
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
