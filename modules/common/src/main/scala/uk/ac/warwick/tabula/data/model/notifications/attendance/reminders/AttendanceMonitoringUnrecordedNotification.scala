package uk.ac.warwick.tabula.data.model.notifications.attendance.reminders

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, TermService}
import uk.ac.warwick.userlookup.User

abstract class AbstractAttendanceMonitoringUnrecordedNotification
	extends Notification[Department, Unit]
	with SingleItemNotification[Department]
	with AllCompletedActionRequiredNotification {

	override final def verb = "record"

	priority = NotificationPriority.Critical

	@transient
	implicit var termService = Wire[TermService]

	@transient
	var attendanceMonitoringService = Wire[AttendanceMonitoringService]

	@transient
	var moduleAndDepartmentService = Wire[ModuleAndDepartmentService]

	final def referenceDate = created.plusDays(-7)

	@transient
	lazy val academicYear = AcademicYear.findAcademicYearContainingDate(referenceDate)

	final def department = item.entity

}

@Entity
@DiscriminatorValue(value="AttendanceMonitoringUnrecordedPoints")
class AttendanceMonitoringUnrecordedPointsNotification
	extends AbstractAttendanceMonitoringUnrecordedNotification {

	override final def url = Routes.View.pointsUnrecorded(department, academicYear)

	override final def urlTitle = "record attendance for these points"

	override def title = {
		val pointsCount = unrecordedPoints.groupBy(p => (p.name, p.startDate, p.endDate)).size

		s"$pointsCount monitoring ${if (pointsCount == 1) "point needs" else "points need"} recording"
	}

	final def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/attendancemonitoring/attendance_monitoring_unrecorded_points_notification.ftl"

	@transient
	final lazy val unrecordedPoints = {
		attendanceMonitoringService.findUnrecordedPoints(department, academicYear, referenceDate.toLocalDate)
	}

	override def content: FreemarkerModel = FreemarkerModel(FreemarkerTemplate, Map(
		"department" -> department,
		"academicYear" -> academicYear,
		"points" -> unrecordedPoints.groupBy(p => (p.name, p.startDate, p.endDate)).map{case(_, groupedPoints) => groupedPoints.head}
	))

	override final def recipients: Seq[User] = {
		if (unrecordedPoints.size > 0) {
			// department.owners is not populated correctly if department not fetched directly
			moduleAndDepartmentService.getDepartmentById(department.id).get.owners.users
		} else {
			Seq()
		}
	}

}

@Entity
@DiscriminatorValue(value="AttendanceMonitoringUnrecordedStudents")
class AttendanceMonitoringUnrecordedStudentsNotification
	extends AbstractAttendanceMonitoringUnrecordedNotification {

	override final def url = Routes.View.studentsUnrecorded(department, academicYear)

	override final def urlTitle = "record attendance for these students"

	override def title = {
		val studentsCount = unrecordedStudents.size

		s"$studentsCount ${if (studentsCount == 1) "student needs" else "students need"} monitoring points recording"
	}

	final def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/attendancemonitoring/attendance_monitoring_unrecorded_students_notification.ftl"

	@transient
	final lazy val unrecordedStudents = {
		attendanceMonitoringService.findUnrecordedStudents(department, academicYear, referenceDate.toLocalDate)
			.sortBy(u => (u.lastName, u.firstName))
	}

	override def content: FreemarkerModel = FreemarkerModel(FreemarkerTemplate, Map(
		"department" -> department,
		"academicYear" -> academicYear,
		"students" -> unrecordedStudents,
		"truncatedStudents" -> unrecordedStudents.slice(0, 10)
	))

	@transient
	override def recipients: Seq[User] =
		if (unrecordedStudents.size > 0) {
			// department.owners is not populated correctly if department not fetched directly
			moduleAndDepartmentService.getDepartmentById(department.id).get.owners.users
		} else {
			Seq()
		}
}