package uk.ac.warwick.tabula.coursework.services.feedbackreport

import scala.collection.JavaConversions._
import collection.immutable.TreeMap

import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import org.joda.time.DateTime

import uk.ac.warwick.tabula.helpers.SpreadsheetHelpers._
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Feedback}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{SubmissionService, FeedbackService, AssignmentMembershipService, AuditEventQueryMethods}
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl

class FeedbackReport(department: Department, startDate: DateTime, endDate: DateTime) {
	import FeedbackReport._

	var auditEventQueryMethods = Wire[AuditEventQueryMethods]
	var assignmentMembershipService = Wire[AssignmentMembershipService]
	var submissionService = Wire[SubmissionService]
	var feedbackService = Wire[FeedbackService]
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
		addStringCell("Credit bearing", header, style)
		addStringCell("Expected submissions", header, style)
		addStringCell("Actual submissions", header, style)
		addStringCell("Late submissions - within extension", header, style)
		addStringCell("Late submissions - without extension", header, style)
		addStringCell("Outstanding feedback", header, style)
		addStringCell("Total published feedback", header, style)
		addStringCell("On-time feedback", header, style)
		addStringCell("On-time feedback %", header, style)
		addStringCell("Late feedback", header, style)
		addStringCell("Late feedback %", header, style)
		addStringCell("Earliest publish date", header, style)
		addStringCell("Latest publish date", header, style)

		sheet
	}

	def populateAssignmentSheet(sheet: XSSFSheet) {
		for (assignmentInfo <- assignmentData) {
			val assignment = assignmentInfo.assignment

			val row = sheet.createRow(sheet.getLastRowNum + 1)
			addStringCell(assignment.name, row)
			addStringCell(assignment.module.code.toUpperCase, row)
			addDateCell(assignment.closeDate, row, dateCellStyle(workbook))
			val publishDeadline = workingDaysHelper.datePlusWorkingDays(assignment.closeDate.toLocalDate, Feedback.PublishDeadlineInWorkingDays)
			addDateCell(publishDeadline, row, dateCellStyle(workbook))
			addStringCell(if (assignment.summative) "Summative" else "Formative", row)
			val numberOfStudents = assignmentMembershipService.determineMembershipUsers(assignment).size
			addNumericCell(numberOfStudents, row)
			addNumericCell(assignment.submissions.size, row)
			addNumericCell(assignment.submissions.filter(submission => submission.isAuthorisedLate).size, row)
			addNumericCell(assignment.submissions.filter(submission => submission.isLate && !submission.isAuthorisedLate).size, row)
			val feedbackCount = getFeedbackCount(assignment)
			val totalPublished = feedbackCount.onTime + feedbackCount.late
			val totalUnPublished = assignment.submissions.size - totalPublished
			addNumericCell(totalUnPublished, row)
			addNumericCell(totalPublished, row)
			addNumericCell(feedbackCount.onTime, row)
			addPercentageCell(feedbackCount.onTime, totalPublished, row, workbook)
			addNumericCell(feedbackCount.late, row)
			addPercentageCell(feedbackCount.late, totalPublished, row, workbook)
			addDateCell(feedbackCount.earliest, row, dateCellStyle(workbook))
			addDateCell(feedbackCount.latest, row, dateCellStyle(workbook))
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
			val feedbackCount = getFeedbackCount(assignment)
			val totalPublished = feedbackCount.onTime + feedbackCount.late
			val assignmentInfo = AssignmentInfo(
				assignment.module.code,
				assignment.module.name,
				assignmentMembershipService.determineMembershipUsers(assignment).size,
				assignment.summative,
				assignment.submissions.size,
				assignment.submissions.filter(submission => submission.isAuthorisedLate).size,
				assignment.submissions.filter(submission => submission.isLate && !submission.isAuthorisedLate).size,
				feedbackCount,
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
		addStringCell("Outstanding Feedback", header, style)
		addStringCell("Total Published Feedback", header, style)
		addStringCell("On-time feedback", header, style)
		addStringCell("On-time feedback %", header, style)
		addStringCell("Late feedback", header, style)
		addStringCell("Late feedback %", header, style)

		sheet
	}


	def populateModuleSheet(sheet: XSSFSheet) {
		val modules = assignmentData.groupBy(_.assignment.module.code)
		val sortedModules = TreeMap(modules.toSeq:_*)
		for ((moduleCode, assignmentInfoList) <- sortedModules) {
			val row = sheet.createRow(sheet.getLastRowNum + 1)

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
			val totalUnPublished = numberOfSubmissions - totalPublished
			addNumericCell(totalUnPublished, row)
			addNumericCell(totalPublished, row)
			val ontime = assignmentInfoList.map(_.feedbackCount.onTime).sum
			addNumericCell(ontime, row)
			addPercentageCell(ontime, totalPublished, row, workbook)
			val late = assignmentInfoList.map(_.feedbackCount.late).sum
			addNumericCell(late, row)
			addPercentageCell(late, totalPublished, row, workbook)
		}
	}


	/**
	 * Returns a tuple:
	 * - first is number of on time feedback
	 * - second is number of late
	 * - third is earliest publish date
	 * - fourth is latest publish date
	 */
	def getFeedbackCount(assignment: Assignment): FeedbackCount =  {
		val times: Seq[FeedbackCount] = for {
			student <- assignmentMembershipService.determineMembershipUsers(assignment)
			submission <- submissionService.getSubmissionByUniId(assignment, student.getWarwickId)
			feedback <- feedbackService.getFeedbackByUniId(assignment, student.getWarwickId)
			if feedback.released
			submissionEventDate <- Option(submission.submittedDate)
			publishEventDate <- Option(feedback.releasedDate).orElse { auditEventQueryMethods.publishFeedbackForStudent(assignment, student).headOption.map { _.eventDate } }
			assignmentCloseDate <- Option(assignment.closeDate)
		} yield {
			val submissionCandidateDate =
				if(submissionEventDate.isAfter(assignmentCloseDate)) submissionEventDate
				else assignmentCloseDate

			// was feedback returned within 20 working days?
			val numOfDays = workingDaysHelper.getNumWorkingDays(submissionCandidateDate.toLocalDate, publishEventDate.toLocalDate)

			// note +1 working day  - getNumWorkingDays is inclusive (starts at 1)
			// we want n working days after the close date
			if (numOfDays > (Feedback.PublishDeadlineInWorkingDays + 1)) FeedbackCount(0, 1, publishEventDate, publishEventDate) // was late
			else FeedbackCount(1, 0, publishEventDate, publishEventDate) // on time
		}

		// merge our list of pairs into a single pair of (on time, late)
		times.foldLeft(FeedbackCount(0, 0, null, null)) { (a, b) =>
			val onTime = a.onTime + b.onTime
			val late = a.late + b.late
			val earliest =
				if (a.earliest == null) b.earliest
				else if (b.earliest == null) a.earliest
				else if (a.earliest.isBefore(b.earliest)) a.earliest
				else b.earliest
			val latest =
				if (a.latest == null) b.latest
				else if (b.latest == null) a.latest
				else if (a.latest.isAfter(b.latest)) a.latest
				else b.latest

			FeedbackCount(onTime, late, earliest, latest)
		}
	}
}

object FeedbackReport {

	val AssignmentSheetSize = 15
	val ModuleSheetSize = 11

	case class FeedbackCount(
		val onTime: Int,
		val late: Int,
		val earliest: DateTime,
		val latest: DateTime
	)

	case class AssignmentInfo (
		val moduleCode: String,
		val moduleName: String,
		val membership: Int,
		val summative: Boolean,
		val numberOfSubmissions: Int,
		val submissionsLateWithExt: Int,
		val submissionsLateWithoutExt: Int,
		val feedbackCount: FeedbackCount,
		val totalPublished: Int,
		val assignment: Assignment
	)

}
