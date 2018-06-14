package uk.ac.warwick.tabula.services.coursework.feedbackreport

import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{Assignment, Department, FeedbackReportGenerator}
import uk.ac.warwick.tabula.helpers.SpreadsheetHelpers._
import uk.ac.warwick.tabula.services.elasticsearch.AuditEventQueryMethods
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, FeedbackService, SubmissionService}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.immutable.TreeMap
import scala.concurrent.duration._
import scala.concurrent.{Await, TimeoutException}

class FeedbackReport(department: Department, academicYear: Option[AcademicYear], startDate: DateTime, endDate: DateTime) {
	import FeedbackReport._

	var auditEventQueryMethods: AuditEventQueryMethods = Wire[AuditEventQueryMethods]
	var assignmentMembershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]
	var submissionService: SubmissionService = Wire[SubmissionService]
	var feedbackService: FeedbackService = Wire[FeedbackService]
	val workbook = new SXSSFWorkbook

	var assignmentData : List[AssignmentInfo] = List()

	def generateAssignmentSheet(dept: Department): Sheet = {
		val sheet = workbook.createSheet("Report for " + safeDeptName(department))
		sheet.trackAllColumnsForAutoSizing()

		// add header row
		val header = sheet.createRow(0)
		val style = headerStyle(workbook)

		// Columns we believe are required for admin report to The Centre TAB-6246
		addStringCell("Assignment name", header, style)
		addStringCell("Module code", header, style)
		addStringCell("Module name", header, style)
		addStringCell("Did assignment meet required 20 University working days turnaround? (Y/N)", header, style)
		addStringCell("Exemption", header, style)
		addStringCell("Notes", header, style)

		// Other columns
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

	def populateAssignmentSheet(sheet: Sheet) {
		for (assignmentInfo <- assignmentData) {
			val row = sheet.createRow(sheet.getLastRowNum + 1)

			// Columns we believe are required for admin report to The Centre TAB-6246
			addStringCell(assignmentInfo.assignment.name, row)
			addStringCell(assignmentInfo.moduleCode.toUpperCase, row)
			addStringCell(assignmentInfo.moduleName, row)
			addStringCell(if (assignmentInfo.feedbackCount.late == 0) "Y" else "N", row)
			addStringCell(if (assignmentInfo.dissertation) "Y" else "N", row)
			addStringCell("", row)

			// Other columns
			addDateCell(assignmentInfo.assignment.closeDate, row, dateCellStyle(workbook))

			val publishDeadline = assignmentInfo.assignment.feedbackDeadline.orNull
			addDateCell(publishDeadline, row, dateCellStyle(workbook))

			addStringCell(if (assignmentInfo.summative) "Summative" else "Formative", row)

			addNumericCell(assignmentInfo.membership, row)
			addNumericCell(assignmentInfo.numberOfSubmissions, row)
			addNumericCell(assignmentInfo.submissionsLateWithExt, row)
			addNumericCell(assignmentInfo.submissionsLateWithoutExt, row)

			addNumericCell(assignmentInfo.membership - assignmentInfo.totalPublished, row)
			addNumericCell(assignmentInfo.totalPublished, row)
			addNumericCell(assignmentInfo.feedbackCount.onTime, row)
			addPercentageCell(assignmentInfo.feedbackCount.onTime, assignmentInfo.totalPublished, row, workbook)
			addNumericCell(assignmentInfo.feedbackCount.late, row)
			addPercentageCell(assignmentInfo.feedbackCount.late, assignmentInfo.totalPublished, row, workbook)
			addDateCell(assignmentInfo.feedbackCount.earliest, row, dateCellStyle(workbook))
			addDateCell(assignmentInfo.feedbackCount.latest, row, dateCellStyle(workbook))
		}
	}

	def buildAssignmentData() {
		val allAssignments = department.modules.asScala.flatMap(_.assignments.asScala).filter(a => academicYear.isEmpty || academicYear.contains(a.academicYear))
		val inDateAssignments = allAssignments.filter(a => ((a.collectSubmissions && a.submissions.size > 0) || (!a.collectSubmissions && a.includeInFeedbackReportWithoutSubmissions))
			&& a.closeDate != null && a.closeDate.isAfter(startDate) && a.closeDate.isBefore(endDate)).toList
		val sortedAssignments = inDateAssignments.sortWith{(a1, a2) =>
			a1.module.code < a2.module.code || (a1.module.code == a2.module.code && a1.closeDate.isBefore(a2.closeDate))
		}

		for (assignment <- sortedAssignments) {
			val feedbackCount = getFeedbackCount(assignment)
			val totalPublished = feedbackCount.onTime + feedbackCount.late

			val membership = assignmentMembershipService.determineMembershipUsers(assignment).size

			val totalSubmissionsCount =
				if (assignment.collectSubmissions) assignment.submissions.size
				else membership

			val assignmentInfo = AssignmentInfo(
				assignment.module.code,
				assignment.module.name,
				membership,
				assignment.summative,
				assignment.dissertation,
				totalSubmissionsCount,
				assignment.submissions.asScala.count(submission => submission.isAuthorisedLate),
				assignment.submissions.asScala.count(submission => submission.isLate && !submission.isAuthorisedLate),
				feedbackCount,
				totalPublished,
				assignment
			)

			assignmentData = assignmentData ++ List(assignmentInfo)
		}
	}

	def generateModuleSheet(dept: Department): Sheet = {
		val sheet = workbook.createSheet("Module report for " + safeDeptName(department))
		sheet.trackAllColumnsForAutoSizing()

		val style = headerStyle(workbook)
		// add header row
		val header = sheet.createRow(0)

		// Columns we believe are required for admin report to The Centre TAB-6246
		addStringCell("Module name", header, style)
		addStringCell("Module code", header, style)
		addStringCell("Did module meet required 20 University working days turnaround? (Y/N)", header, style)
		addStringCell("Any assignments with exemption", header, style)
		addStringCell("Notes", header, style)

		// Other columns
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


	def populateModuleSheet(sheet: Sheet) {
		val modules = assignmentData.groupBy(_.assignment.module.code)
		val sortedModules = TreeMap(modules.toSeq:_*)
		for ((moduleCode, assignmentInfoList) <- sortedModules) {
			val row = sheet.createRow(sheet.getLastRowNum + 1)

			val expectedSubmissions = assignmentInfoList.map(_.membership).sum
			val numberOfSubmissions = assignmentInfoList.map(_.numberOfSubmissions).sum
			val submissionsLateWithExt = assignmentInfoList.map(_.submissionsLateWithExt).sum
			val submissionsLateWithoutExt = assignmentInfoList.map(_.submissionsLateWithoutExt).sum

			val totalPublished = assignmentInfoList.map(_.totalPublished).sum
			val totalUnPublished = numberOfSubmissions - totalPublished
			val ontime = assignmentInfoList.map(_.feedbackCount.onTime).sum
			val late = assignmentInfoList.map(_.feedbackCount.late).sum // minus any dissertations?

			// Columns we believe are required for admin report to The Centre TAB-6246
			addStringCell(assignmentInfoList.head.moduleName, row)
			addStringCell(moduleCode.toUpperCase, row)
			addStringCell(if (late == 0) "Y" else "N", row)
			addStringCell(if (assignmentInfoList.exists(_.assignment.dissertation)) "Y" else "N", row)
			addStringCell("", row)

			// Other columns
			addNumericCell(assignmentInfoList.groupBy(_.assignment.name).size, row)
			addNumericCell(expectedSubmissions, row)
			addNumericCell(numberOfSubmissions, row)
			addNumericCell(submissionsLateWithExt, row)
			addNumericCell(submissionsLateWithoutExt, row)

			// totalUnPublished vs. outstanding. not necessarily the same thing.

			addNumericCell(totalUnPublished, row)
			addNumericCell(totalPublished, row)
			addNumericCell(ontime, row)
			addPercentageCell(ontime, totalPublished, row, workbook)
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
		val submissions: Seq[FeedbackReportGenerator] =
			if (assignment.collectSubmissions) submissionService.getSubmissionsByAssignment(assignment)
			else assignmentMembershipService.determineMembershipUsers(assignment).map(SubmissionlessFeedbackReportGenerator(assignment, _))

		val times: Seq[FeedbackCount] = for {
			submission <- submissions
			feedback <- feedbackService.getAssignmentFeedbackByUsercode(assignment, submission.usercode)
			if feedback.released
			publishEventDate <- Option(feedback.releasedDate).orElse {
				try {
					Await.result(
						auditEventQueryMethods.publishFeedbackForStudent(assignment, feedback.usercode, feedback.universityId),
						5.seconds
					).headOption.map { _.eventDate }
				} catch { case timeout: TimeoutException => None }
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

	case class SubmissionlessFeedbackReportGenerator(assignment: Assignment, user: User) extends FeedbackReportGenerator {
		val usercode: String = user.getUserId
		val feedbackDeadline: Option[LocalDate] = assignment.feedbackDeadline
	}

}
