package uk.ac.warwick.tabula.scheduling.commands

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.services.{TermService, TermServiceComponent}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.{Fixtures, AcademicYear, Mockito, TestBase}
import uk.ac.warwick.util.termdates.Term.TermType
import uk.ac.warwick.util.termdates.TermImpl

class UnlinkAttendanceMonitoringSchemeCommandTest extends TestBase with Mockito {

	@Test
	def commandApply(): Unit = {
		val autumnTerm = new TermImpl(null, new DateTime(2014, 10, 1, 0, 0), null, TermType.autumn)

		val student1 = Fixtures.student("1111", "1111")
		val student2 = Fixtures.student("2222", "2222")
		val student3 = Fixtures.student("3333", "3333")

		val dept1 = Fixtures.department("its")

		val dept1scheme1 = new AttendanceMonitoringScheme
		dept1scheme1.department = dept1
		dept1scheme1.academicYear = AcademicYear(2014)
		dept1scheme1.members = UserGroup.ofUniversityIds
		dept1scheme1.members.staticUserIds = Seq(student1.userId, student2.userId)
		dept1scheme1.members.includedUserIds = Seq(student3.userId)
		dept1scheme1.members.excludedUserIds = Seq(student2.userId)
		dept1scheme1.memberQuery = "some-filter"

		val command = new UnlinkAttendanceMonitoringSchemeCommandInternal with TermServiceComponent with AttendanceMonitoringServiceComponent {
			val termService: TermService = smartMock[TermService]
			val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
		}

		command.termService.getTermFromDateIncludingVacations(any[DateTime]) returns autumnTerm

		command.attendanceMonitoringService.findSchemesLinkedToSITSByDepartment(AcademicYear(2014)) returns Map(
			dept1 -> Seq(dept1scheme1)
		)

		val result = command.applyInternal()
		result.get(dept1).isDefined should be {true}
		result(dept1).size should be (1)
		result(dept1).head.members.members.size should be (2)
		result(dept1).head.members.includesUserId(student1.userId) should be {true}
		result(dept1).head.members.includesUserId(student2.userId) should be {false}
		result(dept1).head.members.includesUserId(student3.userId) should be {true}
		verify(command.attendanceMonitoringService, times(1)).saveOrUpdate(dept1scheme1)
	}

}
