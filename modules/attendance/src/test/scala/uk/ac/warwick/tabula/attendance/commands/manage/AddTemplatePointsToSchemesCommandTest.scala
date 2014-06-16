package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringScheme, AttendanceMonitoringTemplate, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.services.{AttendanceMonitoringServiceComponent, AttendanceMonitoringService, TermService, TermServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}


class AddTemplatePointsToSchemesCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends AddTemplatePointsToSchemesCommandState with TermServiceComponent with
	AttendanceMonitoringServiceComponent {
		val termService = mock[TermService]
		val attendanceMonitoringService = mock[AttendanceMonitoringService]

		val scheme = new AttendanceMonitoringScheme
		val scheme1 = new AttendanceMonitoringScheme
		schemes.add(scheme)
		schemes.add(scheme1)

	}

	trait Fixture {
		val academicYear = new AcademicYear(2014)
		val department = new Department
	}

	@Test
	def addPointsFromTemplate() {
		new Fixture {
			val points = Seq(new AttendanceMonitoringPoint, new AttendanceMonitoringPoint, new AttendanceMonitoringPoint)
			val templateScheme = new AttendanceMonitoringTemplate

			val command = new AddTemplatePointsToSchemesCommandInternal(department, academicYear) with CommandTestSupport
			command.templateScheme = templateScheme
			command.attendanceMonitoringService.generatePointsFromTemplateScheme(templateScheme, academicYear) returns points

			val newPoints = command.applyInternal()
			newPoints.size should be (6)
		}
	}

}
