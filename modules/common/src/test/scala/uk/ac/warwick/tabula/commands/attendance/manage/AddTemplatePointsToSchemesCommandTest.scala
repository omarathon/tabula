package uk.ac.warwick.tabula.commands.attendance.manage

import org.joda.time.DateTime
import org.mockito.Matchers
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{ScheduledNotification, Department}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringScheme, AttendanceMonitoringTemplate, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AttendanceMonitoringService}
import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import org.hamcrest.Matchers._


class AddTemplatePointsToSchemesCommandTest extends TestBase with Mockito {

	val academicYear = new AcademicYear(2014)
	val department = new Department

	trait CommandTestSupport extends AddTemplatePointsToSchemesCommandState with TermServiceComponent
		with AttendanceMonitoringServiceComponent with ProfileServiceComponent {
		val termService = smartMock[TermService]
		val attendanceMonitoringService = smartMock[AttendanceMonitoringService]
		val profileService = smartMock[ProfileService]

		templateScheme = new AttendanceMonitoringTemplate
		templateScheme.pointStyle = AttendanceMonitoringPointStyle.Date

		val baseDate = DateTime.now.toLocalDate

		val point1 = new AttendanceMonitoringPoint
		point1.name = "point1"
		point1.startDate = baseDate
		point1.endDate = baseDate.plusDays(1)

		val point2 = new AttendanceMonitoringPoint
		point2.name = "point2"
		point2.startDate = baseDate
		point2.endDate = baseDate.plusDays(1)

		val point3 = new AttendanceMonitoringPoint
		point3.name = "point3"
		point3.startDate = baseDate
		point3.endDate = baseDate.plusDays(1)

		val points = Seq(point1, point2, point3)

		val scheme = new AttendanceMonitoringScheme
		scheme.department = department
		scheme.pointStyle = AttendanceMonitoringPointStyle.Date

		val scheme1 = new AttendanceMonitoringScheme
		scheme1.department = department
		scheme1.pointStyle = AttendanceMonitoringPointStyle.Date

		schemes.add(scheme)
		schemes.add(scheme1)

		attendanceMonitoringService.generatePointsFromTemplateScheme(templateScheme, academicYear) returns points
		profileService.getAllMembersWithUniversityIds(anArgThat(anything)) returns Nil
		attendanceMonitoringService.listAllSchemes(department) returns Seq(scheme)

	}

	trait Fixture {

		val command = new AddTemplatePointsToSchemesCommandInternal(department, academicYear) with CommandTestSupport with AddTemplatePointsToSchemesValidation
		val errors = new BindException(command, "command")
		command.thisScheduledNotificationService = smartMock[ScheduledNotificationService]
	}

	@Test
	def addPointsFromTemplate() {
		new Fixture {
			val newPoints = command.applyInternal()
			newPoints.size should be (6)
			verify(command.thisScheduledNotificationService, times(1)).removeInvalidNotifications(department)
			verify(command.thisScheduledNotificationService, atLeast(1)).push(Matchers.any[ScheduledNotification[Department]])
		}
	}

	@Test
	def validateAll() {
		new Fixture {
			command.validate(errors)
			errors.getErrorCount should be (0)
		}
	}

	@Test
	def noTemplateScheme() {
		new Fixture {
			command.templateScheme = null
			command.validate(errors)
			errors.getErrorCount should be (1)
		}
	}

	@Test
	def mixedSchemeType() {
		new Fixture {
			command.scheme1.pointStyle = AttendanceMonitoringPointStyle.Week
			command.validate(errors)
			errors.getErrorCount should be (1)
		}
	}

	@Test
	def duplicatePoints() {
		new Fixture {
			command.scheme1.points.add(command.point1)
			command.validate(errors)
			errors.getErrorCount should be (1)
		}
	}

}
