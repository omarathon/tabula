package uk.ac.warwick.tabula.coursework.services.feedbackreport

import scala.collection.JavaConverters._
import collection.immutable.TreeMap

import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import org.joda.time.DateTime

import uk.ac.warwick.tabula.helpers.SpreadsheetHelpers._
import uk.ac.warwick.tabula.data.model.{Assignment, Department}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{SubmissionService, FeedbackService, AssignmentMembershipService, AuditEventQueryMethods}

class FeedbackReport(department: Department, startDate: DateTime, endDate: DateTime) {
	import FeedbackReport._

	var auditEventQueryMethods = Wire[AuditEventQueryMethods]
	var assignmentMembershipService = Wire[AssignmentMembershipService]
	var submissionService = Wire[SubmissionService]
	var feedbackService = Wire[FeedbackService]
	val workbook = new XSSFWorkbook()

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
		addStringCell("Is dissertation?", header, style)
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
			val publishDeadline = assignment.feedbackDeadline.orNull
			addDateCell(publishDeadline, row, dateCellStyle(workbook))
			addStringCell(if (assignment.summative) "Summative" else "Formative", row)
			addStringCell(if (assignment.dissertation) "Dissertation" else "", row)
			val numberOfStudents = assignmentMembershipService.determineMembershipUsers(assignment).size
			addNumericCell(numberOfStudents, row)
			addNumericCell(assignment.submissions.size, row)
			addNumericCell(assignment.submissions.asScala.count(submission => submission.isAuthorisedLate), row)
			addNumericCell(assignment.submissions.asScala.count(submission => submission.isLate && !submission.isAuthorisedLate), row)
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

		val allAssignments = department.modules.asScala.flatMap(_.assignments.asScala)
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
				assignment.dissertation,
				assignment.submissions.size,
				assignment.submissions.asScala.count(submission => submission.isAuthorisedLate),
				assignment.submissions.asScala.count(submission => submission.isLate && !submission.isAuthorisedLate),
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
			// totalUnPublished vs. outstanding. not necessarily the same thing.

			addNumericCell(totalUnPublished, row)
			addNumericCell(totalPublished, row)
			val ontime = assignmentInfoList.map(_.feedbackCount.onTime).sum
			addNumericCell(ontime, row)
			addPercentageCell(ontime, totalPublished, row, workbook)
			val late = assignmentInfoList.map(_.feedbackCount.late).sum // minus any dissertations?
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
			submission <- submissionService.getSubmissionsByAssignment(assignment)
			feedback <- feedbackService.getFeedbackByUniId(assignment, submission.universityId)
			if feedback.released
			publishEventDate <- Option(feedback.releasedDate).orElse {
				auditEventQueryMethods.publishFeedbackForStudent(assignment, feedback.universityId).headOption.map { _.eventDate }
			}
		} yield {
			// was feedback returned within 20 working days?
			submission.feedbackDeadline // If the deadline is exempt (e.g. open-ended or dissertation) this will return None
				.filter(publishEventDate.toLocalDate.isAfter)
				.map { _ =>  FeedbackCount(0, 1, publishEventDate, publishEventDate) } // was late
				.getOrElse { FeedbackCount(1, 0, publishEventDate, publishEventDate) } // on time
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
		onTime: Int,
		late: Int,
		earliest: DateTime,
		latest: DateTime
	)

	case class AssignmentInfo (
		moduleCode: String,
		moduleName: String,
		membership: Int,
		summative: Boolean,
		var dissertation: Boolean,
		numberOfSubmissions: Int,
		submissionsLateWithExt: Int,
		submissionsLateWithoutExt: Int,
		feedbackCount: FeedbackCount,
		totalPublished: Int,
		assignment: Assignment
	)

}
