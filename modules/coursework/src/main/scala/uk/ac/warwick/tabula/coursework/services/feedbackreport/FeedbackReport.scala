package uk.ac.warwick.tabula.coursework.services.feedbackreport

import uk.ac.warwick.tabula.helpers.SpreadsheetHelper
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import uk.ac.warwick.tabula.data.model.{Assignment, Department}
import org.apache.poi.ss.util.WorkbookUtil
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{AssignmentMembershipService, AuditEventQueryMethods}
import collection.JavaConversions._
import collection.immutable.TreeMap
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl
import org.joda.time.DateTime

class FeedbackReport(department: Department, startDate: DateTime, endDate: DateTime)
	extends SpreadsheetHelper {

	val assignmentSheetSize = 13
	val moduleSheetSize = 11

	var auditEventQueryMethods = Wire.auto[AuditEventQueryMethods]
	var assignmentMembershipService = Wire.auto[AssignmentMembershipService]
	val workbook = new XSSFWorkbook()
	var workingDaysHelper = new WorkingDaysHelperImpl

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

	def generateAssignmentSheet(dept: Department) = {
		val sheet = workbook.createSheet("Report for " + safeDeptName)

		// add header row
		val header = sheet.createRow(0)
		val style = headerStyle(workbook)

		addStringCell("Assignment name", header, style)
		addStringCell("Module code", header, style)
		addStringCell("Close date", header, style)
		addStringCell("Publish deadline", header, style)
		addStringCell("Expected submissions", header, style)
		addStringCell("Actual submissions", header, style)
		addStringCell("Late submissions - within extension", header, style)
		addStringCell("Late submissions - without extension", header, style)
		addStringCell("Total published feedback", header, style)
		addStringCell("On-time feedback", header, style)
		addStringCell("On-time feedback %", header, style)
		addStringCell("Late feedback", header, style)
		addStringCell("Late feedback %", header, style)

		sheet
	}

	def populateAssignmentSheet(sheet: XSSFSheet) {
		for (assignmentInfo <- assignmentData) {

			val assignment = assignmentInfo.assignment

			val row = sheet.createRow(sheet.getLastRowNum + 1)
			addStringCell(assignment.name, row)
			addStringCell(assignment.module.code.toUpperCase, row)
			addDateCell(assignment.closeDate.toDate, row, dateCellStyle(workbook))
			val publishDeadline = workingDaysHelper.datePlusWorkingDays(assignment.closeDate.toLocalDate, 20)
			addDateCell(publishDeadline.toDate, row, dateCellStyle(workbook))
			val numberOfStudents = assignmentMembershipService.determineMembershipUsers(assignment).size
			addNumericCell(numberOfStudents, row)
			addNumericCell(assignment.submissions.size, row)
			addNumericCell(assignment.submissions.filter(submission => submission.isAuthorisedLate).size, row)
			addNumericCell(assignment.submissions.filter(submission => submission.isLate && !submission.isAuthorisedLate).size, row)
			val (onTime, late) = getFeedbackCounts(assignment)
			val totalPublished = onTime + late
			addNumericCell(totalPublished, row)
			addNumericCell(onTime, row)
			addPercentageCell(onTime, totalPublished, row, workbook)
			addNumericCell(late, row)
			addPercentageCell(late, totalPublished, row, workbook)
		}
	}

	def buildAssignmentData() {

		val allAssignments = department.modules.flatMap(_.assignments)
		val inDateAssignments = allAssignments.filter(a => a.collectSubmissions && a.submissions.size > 0
			&& a.closeDate.isAfter(startDate) && a.closeDate.isBefore(endDate)).toList
		val sortedAssignments = inDateAssignments.sortWith{(a1, a2) =>
			a1.module.code < a2.module.code || (a1.module.code == a2.module.code && a1.closeDate.isBefore(a2.closeDate))
		}

		for (assignment <- sortedAssignments) {

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

	def generateModuleSheet(dept: Department) = {
		val sheet = workbook.createSheet("Module report for " + safeDeptName)
		val style = headerStyle(workbook)
		// add header row
		val header = sheet.createRow(0)
		addStringCell("Module name", header, style)
		addStringCell("Module code", header, style)
		addStringCell("Number of assignments", header, style)
		addStringCell("Expected submissions", header, style)
		addStringCell("Actual submissions", header, style)
		addStringCell("Late submissions - within extension", header, style)
		addStringCell("Late submissions - without extension", header, style)
		addStringCell("On-time feedback", header, style)
		addStringCell("On-time feedback %", header, style)
		addStringCell("Late feedback", header, style)
		addStringCell("Late feedback %", header, style)

		sheet
	}


	def populateModuleSheet(sheet: XSSFSheet) {
		val modules = assignmentData.groupBy(_.assignment.module.code)
		val sortedModules = TreeMap(modules.toSeq:_*)
		for (module <- sortedModules) {
			val row = sheet.createRow(sheet.getLastRowNum + 1)
			val moduleCode = module._1
			val assignmentInfoList= module._2

			addStringCell(assignmentInfoList(0).moduleName, row)
			addStringCell(moduleCode.toUpperCase, row)
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
			addPercentageCell(ontime, totalPublished, row, workbook)
			val late = assignmentInfoList.map(_.lateFeedback).sum
			addNumericCell(late, row)
			addPercentageCell(late, totalPublished, row, workbook)
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
}


