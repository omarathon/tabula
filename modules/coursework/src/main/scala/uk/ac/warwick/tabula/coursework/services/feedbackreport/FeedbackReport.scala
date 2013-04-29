package uk.ac.warwick.tabula.coursework.services.feedbackreport

import uk.ac.warwick.tabula.helpers.SpreadsheetHelpers
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import uk.ac.warwick.tabula.data.model.{Assignment, Department}
import org.apache.poi.ss.util.WorkbookUtil
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{SubmissionService, AssignmentMembershipService, AuditEventQueryMethods}
import collection.JavaConversions._
import collection.immutable.TreeMap
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl
import org.joda.time.DateTime

class FeedbackReport(department: Department, startDate: DateTime, endDate: DateTime)
	extends SpreadsheetHelpers {

	val assignmentSheetSize = 13
	val moduleSheetSize = 11
	
	val PUBLISH_DEADLINE_WORKING_DAYS = 20

	var auditEventQueryMethods = Wire.auto[AuditEventQueryMethods]
	var assignmentMembershipService = Wire.auto[AssignmentMembershipService]
	var submissionService = Wire.auto[SubmissionService]
	val workbook = new XSSFWorkbook()
	var workingDaysHelper = new WorkingDaysHelperImpl

		var assignmentData : List[AssignmentInfo] = List()

	def generateAssignmentSheet(dept: Department) = {
		val sheet = workbook.createSheet("Report for " + safeDeptName(department))

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
			val publishDeadline = workingDaysHelper.datePlusWorkingDays(assignment.closeDate.toLocalDate, PUBLISH_DEADLINE_WORKING_DAYS)
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
			val (onTime, late) = getFeedbackCounts(assignment)
			val totalPublished = onTime + late
			val assignmentInfo = new AssignmentInfo(
				assignment.module.code,
				assignment.module.name,
				assignmentMembershipService.determineMembershipUsers(assignment).size,
				assignment.submissions.size,
				assignment.submissions.filter(submission => submission.isAuthorisedLate).size,
				assignment.submissions.filter(submission => submission.isLate && !submission.isAuthorisedLate).size,
				onTime,
				late,
			  totalPublished,
				assignment
			)

			assignmentData = assignmentData ++ List(assignmentInfo)
		}
	}

	def generateModuleSheet(dept: Department) = {
		val sheet = workbook.createSheet("Module report for " + safeDeptName(department))
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


	/**
	 * Returns a pair - first is number of on time feedback, second is number of late
	 */

	def getFeedbackCounts(assignment: Assignment) : (Int, Int) =  {

		val times:Seq[(Int, Int)] = for (
			student <- assignmentMembershipService.determineMembershipUsers(assignment);
			//submissionEvent <- auditEventQueryMethods.submissionForStudent(assignment, student).headOption ;
			submission <- submissionService.getSubmissionByUniId(assignment, student.getWarwickId) ;
			publishEvent <- auditEventQueryMethods.publishFeedbackForStudent(assignment, student).headOption;
			submissionEventDate <- Option(submission.submittedDate);
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
			// note +1 working day  - getNumWorkingDays is inclusive (starts at 1)
			// we want n working days after the close date
			if(numOfDays > (PUBLISH_DEADLINE_WORKING_DAYS + 1)) (0,1) // was late
			else (1,0) // on time
		}
		// merge our list of pairs into a single pair of (on time, late)
		times.foldLeft(0,0)((a,b) => (a._1 + b._1 , a._2 + b._2))
	}


	case class AssignmentInfo (
		var moduleCode: String,
		var moduleName: String,
		var membership: Int,
		var numberOfSubmissions: Int,
		var submissionsLateWithExt: Int,
		var submissionsLateWithoutExt: Int,
		var onTimeFeedback: Int,
		var lateFeedback: Int,
		var totalPublished: Int,
		var assignment: Assignment
	)
}


