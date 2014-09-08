package uk.ac.warwick.tabula.data.model.notifications.attendancemonitoring

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, TermService}
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue(value="AttendanceMonitoringUnrecordedPoints")
class AttendanceMonitoringUnrecordedPointsNotification
	extends NotificationWithTarget[Department, Department]
	with SingleItemNotification[Department] {

	override final def verb = "record"

	override final def actionRequired = true

	priority = NotificationPriority.Critical

	@transient
	val termService = Wire[TermService]

	@transient
	val attendanceMonitoringService = Wire[AttendanceMonitoringService]

	@transient
	val moduleAndDepartmentService = Wire[ModuleAndDepartmentService]

	final def referenceDate = created.plusDays(-7)

	@transient
	lazy val academicYear = AcademicYear.findAcademicYearContainingDate(referenceDate, termService)

	final def department = item.entity

	override final def url = Routes.View.pointsUnrecorded(department, academicYear)

	override final def urlTitle = "record attendance for these points"

	override def title = "Monitoring points need recording"

	final def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/attendancemonitoring/attendance_monitoring_unrecorded_points_notification.ftl"

	@transient
	final lazy val unrecordedPoints = {
		attendanceMonitoringService.findUnrecordedPoints(department, academicYear, referenceDate.toLocalDate)
	}

	override def content: FreemarkerModel = FreemarkerModel(FreemarkerTemplate, Map(
		"department" -> department,
		"academicYear" -> academicYear,
		"points" -> unrecordedPoints
	))

	override def recipients: Seq[User] = {
		if (unrecordedPoints.size > 0) {
			// department.owners is not populated correctly if department not fetched directly
			moduleAndDepartmentService.getDepartmentById(department.id).get.owners.users
		} else {
			Seq()
		}
	}
}