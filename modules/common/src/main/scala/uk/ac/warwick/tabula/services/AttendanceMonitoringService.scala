package uk.ac.warwick.tabula.services

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringScheme}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.{AutowiringAttendanceMonitoringDaoComponent, AttendanceMonitoringDaoComponent}


trait AttendanceMonitoringServiceComponent {
	def attendanceMonitoringService: AttendanceMonitoringService
}

trait AutowiringAttendanceMonitoringServiceComponent extends AttendanceMonitoringServiceComponent {
	val attendanceMonitoringService = Wire[AttendanceMonitoringService]
}

trait AttendanceMonitoringService {
	def getSchemeById(id: String): Option[AttendanceMonitoringScheme]
	def saveOrUpdate(scheme: AttendanceMonitoringScheme): Unit
	def listSchemes(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme]
	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String]
	def studentAlreadyReportedThisTerm(student: StudentMember, point: AttendanceMonitoringPoint): Boolean
}

abstract class AbstractAttendanceMonitoringService extends AttendanceMonitoringService {

	self: AttendanceMonitoringDaoComponent with TermServiceComponent =>

	def getSchemeById(id: String): Option[AttendanceMonitoringScheme] =
		attendanceMonitoringDao.getSchemeById(id)

	def saveOrUpdate(scheme: AttendanceMonitoringScheme): Unit =
		attendanceMonitoringDao.saveOrUpdate(scheme)

	def listSchemes(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme] =
		attendanceMonitoringDao.listSchemes(department, academicYear)

	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String] =
		attendanceMonitoringDao.findNonReportedTerms(students, academicYear)

	def studentAlreadyReportedThisTerm(student: StudentMember, point: AttendanceMonitoringPoint): Boolean =
		findNonReportedTerms(Seq(student), point.scheme.academicYear).contains(
			termService.getTermFromDateIncludingVacations(point.startDate.toDateTimeAtStartOfDay).getTermTypeAsString
		)

}

trait AttendanceMonitoringMembershipHelpers {
	val membersHelper: UserGroupMembershipHelper[AttendanceMonitoringScheme]
}

// new up UGMHs which will Wire.auto() their dependencies
trait AttendanceMonitoringMembershipHelpersImpl extends AttendanceMonitoringMembershipHelpers {
	val membersHelper = new UserGroupMembershipHelper[AttendanceMonitoringScheme]("_members")
}

@Service("attendanceMonitoringService")
class AttendanceMonitoringServiceImpl
	extends AbstractAttendanceMonitoringService
	with AttendanceMonitoringMembershipHelpersImpl
	with AutowiringTermServiceComponent
	with AutowiringAttendanceMonitoringDaoComponent

