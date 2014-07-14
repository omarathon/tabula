package uk.ac.warwick.tabula.services

import org.springframework.core.env.Environment
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceUpdateCheckpointTotalMessage, AttendanceMonitoringServiceListener, AttendanceMonitoringService}
import uk.ac.warwick.tabula.{Fixtures, AcademicYear, Mockito, TestBase}
import uk.ac.warwick.util.queue.Queue

class AttendanceMonitoringServiceListenerTest extends TestBase with Mockito {

	trait TestSupport {
		val queue = smartMock[Queue]
		val env = smartMock[Environment]
		val profileService = smartMock[ProfileService]
		val thisAttendanceMonitoringService = smartMock[AttendanceMonitoringService]
		val moduleAndDepartmentService = smartMock[ModuleAndDepartmentService]

		val listener = new AttendanceMonitoringServiceListener {
			override val attendanceMonitoringService = thisAttendanceMonitoringService
		}
		listener.queue = queue
		listener.env = env
		listener.profileService = profileService
		listener.moduleAndDepartmentService = moduleAndDepartmentService

		val uniId = "1234"
		val student = Fixtures.student(uniId)
		val deptCode = "its"
		val department = Fixtures.department(deptCode)
	}

	@Test
	def listeningToQueue() {
		new TestSupport {
			listener.env.acceptsProfiles("dev", "scheduling") returns true
			listener.isListeningToQueue should be {true}
		}
	}

	@Test
	def onReceiveNotStudent() {
		new TestSupport {
			listener.profileService.getMemberByUniversityIdStaleOrFresh(uniId) returns None
			listener.moduleAndDepartmentService.getDepartmentByCode(deptCode) returns Option(department)
			listener.onReceive(new AttendanceMonitoringServiceUpdateCheckpointTotalMessage(uniId, deptCode, null))
			there was no (listener.attendanceMonitoringService).updateCheckpointTotal(any[StudentMember], any[Department], any[AcademicYear])
		}

		new TestSupport {
			val staffMember = Fixtures.staff(uniId)
			listener.profileService.getMemberByUniversityIdStaleOrFresh(uniId) returns Option(staffMember)
			listener.moduleAndDepartmentService.getDepartmentByCode(deptCode) returns Option(department)
			listener.onReceive(new AttendanceMonitoringServiceUpdateCheckpointTotalMessage(uniId, deptCode, null))
			there was no (listener.attendanceMonitoringService).updateCheckpointTotal(any[StudentMember], any[Department], any[AcademicYear])
		}
	}

	@Test
	def onReceiveInvalidDepartment() {
		new TestSupport {
			listener.profileService.getMemberByUniversityIdStaleOrFresh(uniId) returns Option(student)
			listener.moduleAndDepartmentService.getDepartmentByCode(deptCode) returns None
			listener.moduleAndDepartmentService.getDepartmentById(deptCode) returns None
			listener.onReceive(new AttendanceMonitoringServiceUpdateCheckpointTotalMessage(uniId, deptCode, null))
			there was no (listener.attendanceMonitoringService).updateCheckpointTotal(any[StudentMember], any[Department], any[AcademicYear])
		}
	}

	@Test
	def onReceiveInvalidAcademicYear() {
		new TestSupport {
			listener.profileService.getMemberByUniversityIdStaleOrFresh(uniId) returns Option(student)
			listener.moduleAndDepartmentService.getDepartmentByCode(deptCode) returns Option(department)
			listener.onReceive(new AttendanceMonitoringServiceUpdateCheckpointTotalMessage(uniId, deptCode, "year"))
			there was no (listener.attendanceMonitoringService).updateCheckpointTotal(any[StudentMember], any[Department], any[AcademicYear])
		}
	}

	@Test
	def onReceiveValid() {
		new TestSupport {
			listener.profileService.getMemberByUniversityIdStaleOrFresh(uniId) returns Option(student)
			listener.moduleAndDepartmentService.getDepartmentByCode(deptCode) returns Option(department)
			listener.onReceive(new AttendanceMonitoringServiceUpdateCheckpointTotalMessage(uniId, deptCode, "2014"))
			there was one (listener.attendanceMonitoringService).updateCheckpointTotal(student, department, AcademicYear(2014))
		}
	}

}
