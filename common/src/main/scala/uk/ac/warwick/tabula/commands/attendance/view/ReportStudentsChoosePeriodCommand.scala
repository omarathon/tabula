package uk.ac.warwick.tabula.commands.attendance.view

import org.joda.time.LocalDate
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.ReportStudentsChoosePeriodCommandNotification
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicPeriod, AcademicYear, ItemNotFoundException}
import uk.ac.warwick.userlookup.User

case class StudentReportCount(student: StudentMember, missed: Int, unrecorded: Int)

case class StudentReport(
	studentReportCounts: Seq[StudentReportCount],
	department: Department,
	currentUser: User
)

object ReportStudentsChoosePeriodCommand {
	def apply(department: Department, academicYear: AcademicYear, currentUser: User) =
		new ReportStudentsChoosePeriodCommandInternal(department, academicYear, currentUser)
			with ComposableCommand[StudentReport]
			with AutowiringProfileServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with ReportStudentsChoosePeriodCommandNotifications
			with ReportStudentsChoosePeriodValidation
			with ReportStudentsChoosePeriodPermissions
			with ReportStudentsChoosePeriodCommandState
			with ReadOnly with Unaudited
}

trait ReportStudentsChoosePeriodCommandNotifications extends Notifies[StudentReport, User] {

	override def emit(result: StudentReport): Seq[ReportStudentsChoosePeriodCommandNotification] = {
		Seq(Notification.init(
			new ReportStudentsChoosePeriodCommandNotification,
			result.currentUser,
			result.department
		))
	}

}

class ReportStudentsChoosePeriodCommandInternal(
	val department: Department,
	val academicYear: AcademicYear,
	val currentUser: User
) extends CommandInternal[StudentReport] {

	self: ReportStudentsChoosePeriodCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal(): StudentReport = {
		StudentReport(
			studentReportCounts = studentMissedReportCounts,
			department = department,
			currentUser = currentUser
		)
	}

}

trait ReportStudentsChoosePeriodValidation extends SelfValidating {

	self: ReportStudentsChoosePeriodCommandState =>

	override def validate(errors: Errors) {
		if (!availablePeriods.filter(_._2).map(_._1).contains(period)) {
			errors.rejectValue("period", "attendanceMonitoringReport.invalidPeriod")
		}
	}

}

trait ReportStudentsChoosePeriodPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ReportStudentsChoosePeriodCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Report, department)
	}

}

trait ReportStudentsChoosePeriodCommandState extends FilterStudentsAttendanceCommandState with TaskBenchmarking {

	self: AttendanceMonitoringServiceComponent =>

	// Only students whose enrolment department is this department
	lazy val allStudents: Seq[StudentMember] = benchmarkTask("profileService.findAllStudentsByRestrictions") {
		profileService.findAllStudentsByRestrictions(
			department = department,
			restrictions = buildRestrictions(academicYear)
		).sortBy(s => (s.lastName, s.firstName))
	}

	lazy val studentPointMap: Map[StudentMember, Seq[AttendanceMonitoringPoint]] = benchmarkTask("studentPointMap") {
		allStudents.map(s => s -> attendanceMonitoringService.listStudentsPoints(s, Option(department), academicYear)).toMap
	}

	lazy val termPoints: Map[String, Seq[AttendanceMonitoringPoint]] = benchmarkTask("termPoints") {
		studentPointMap.values.flatten.toSeq.groupBy { point =>
			academicYear.termOrVacationForDate(point.startDate).periodType.toString
		}.mapValues(_.distinct)
	}

	lazy val availablePeriods: Seq[(String, Boolean)] = benchmarkTask("availablePeriods") {
		val termsWithPoints = termPoints.keys.toSeq
		val orderedTermNames = AcademicPeriod.allPeriodTypes.map(_.toString)

		val thisTerm = {
			if (academicYear < AcademicYear.now())
				orderedTermNames.last
			else
				academicYear.termOrVacationForDate(LocalDate.now).periodType.toString
		}
		val thisTermIndex = orderedTermNames.zipWithIndex
			.find(_._1 == thisTerm).getOrElse(throw new ItemNotFoundException())._2
		val termsSoFarThisYear = orderedTermNames.slice(0, thisTermIndex + 1)
		val nonReportedTerms = attendanceMonitoringService.findNonReportedTerms(allStudents, academicYear)
		val termsToShow = termsSoFarThisYear.intersect(termsWithPoints)
		// Visible terms as those that are this term or before
		// Terms that can be selected are those that no selected student has been reported for
		termsToShow.map(term => term -> nonReportedTerms.contains(term))
	}

	lazy val studentReportCounts: Seq[StudentReportCount] = {
		val relevantPoints = termPoints(period).intersect(studentPointMap.values.flatten.toSeq)
		val checkpoints = attendanceMonitoringService.getCheckpoints(relevantPoints, allStudents)

		allStudents.map { student => {
			// Points the student is taking that are in the given period
			val studentPoints = termPoints(period).intersect(studentPointMap(student))
			val unrecorded = studentPoints.count(point =>
				checkpoints.get(student).flatMap(_.get(point)).isEmpty
			)
			val missedAndUnreported = studentPoints.count(point =>
				checkpoints.get(student).flatMap(_.get(point)).exists(_.state == AttendanceState.MissedUnauthorised)
					&& !attendanceMonitoringService.studentAlreadyReportedThisTerm(student, point)
			)
			StudentReportCount(student, missedAndUnreported, unrecorded)
		}}
	}

	lazy val studentMissedReportCounts: Seq[StudentReportCount] = studentReportCounts.filter(_.missed > 0)

	// Bind variables

	var period: String = _
}
