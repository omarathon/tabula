package uk.ac.warwick.tabula.services

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.attendance._
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.{SchemeMembershipItemType, AutowiringAttendanceMonitoringDaoComponent, AttendanceMonitoringDaoComponent}
import uk.ac.warwick.tabula.data.SchemeMembershipItem
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPointType
import uk.ac.warwick.tabula.commands.MemberOrUser
import collection.JavaConverters._

trait AttendanceMonitoringServiceComponent {
	def attendanceMonitoringService: AttendanceMonitoringService
}

trait AutowiringAttendanceMonitoringServiceComponent extends AttendanceMonitoringServiceComponent {
	val attendanceMonitoringService = Wire[AttendanceMonitoringService]
}

trait AttendanceMonitoringService {
	def getSchemeById(id: String): Option[AttendanceMonitoringScheme]
	def getPointById(id: String): Option[AttendanceMonitoringPoint]
	def saveOrUpdate(scheme: AttendanceMonitoringScheme): Unit
	def saveOrUpdate(point: AttendanceMonitoringPoint): Unit
	def deleteScheme(scheme: AttendanceMonitoringScheme)
	def listSchemes(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme]
	def listOldSets(department: Department, academicYear: AcademicYear): Seq[MonitoringPointSet]
	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String]
	def studentAlreadyReportedThisTerm(student: StudentMember, point: AttendanceMonitoringPoint): Boolean
	def findReports(studentIds: Seq[String], year: AcademicYear, period: String): Seq[MonitoringPointReport]
	def findSchemeMembershipItems(universityIds: Seq[String], itemType: SchemeMembershipItemType): Seq[SchemeMembershipItem]
	def findPoints(
		department: Department,
		academicYear: AcademicYear,
		schemes: Seq[AttendanceMonitoringScheme],
		types: Seq[AttendanceMonitoringPointType],
		styles: Seq[AttendanceMonitoringPointStyle]
	): Seq[AttendanceMonitoringPoint]
	def findOldPoints(
		department: Department,
		academicYear: AcademicYear,
		sets: Seq[MonitoringPointSet],
		types: Seq[AttendanceMonitoringPointType]
	): Seq[MonitoringPoint]
	def listStudentsPoints(student: StudentMember, department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringPoint]
	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]
	def getAttendanceNote(student: StudentMember, point: AttendanceMonitoringPoint): Option[AttendanceMonitoringNote]
	def countCheckpointsForPoint(point: AttendanceMonitoringPoint): Int
}

abstract class AbstractAttendanceMonitoringService extends AttendanceMonitoringService {

	self: AttendanceMonitoringDaoComponent with TermServiceComponent with AttendanceMonitoringMembershipHelpers with UserLookupComponent =>

	def getSchemeById(id: String): Option[AttendanceMonitoringScheme] =
		attendanceMonitoringDao.getSchemeById(id)

	def getPointById(id: String): Option[AttendanceMonitoringPoint] =
		attendanceMonitoringDao.getPointById(id)

	def saveOrUpdate(scheme: AttendanceMonitoringScheme): Unit =
		attendanceMonitoringDao.saveOrUpdate(scheme)

	def saveOrUpdate(point: AttendanceMonitoringPoint): Unit =
		attendanceMonitoringDao.saveOrUpdate(point)

	def deleteScheme(scheme: AttendanceMonitoringScheme) = {
		attendanceMonitoringDao.delete(scheme)
	}

	def listSchemes(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme] =
		attendanceMonitoringDao.listSchemes(department, academicYear)

	def listOldSets(department: Department, academicYear: AcademicYear): Seq[MonitoringPointSet] =
		attendanceMonitoringDao.listOldSets(department, academicYear)

	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String] =
		attendanceMonitoringDao.findNonReportedTerms(students, academicYear)

	def studentAlreadyReportedThisTerm(student: StudentMember, point: AttendanceMonitoringPoint): Boolean =
		findNonReportedTerms(Seq(student), point.scheme.academicYear).contains(
			termService.getTermFromDateIncludingVacations(point.startDate.toDateTimeAtStartOfDay).getTermTypeAsString
		)

	def findReports(studentIds: Seq[String], year: AcademicYear, period: String): Seq[MonitoringPointReport] =
		attendanceMonitoringDao.findReports(studentIds, year, period)

	def findSchemeMembershipItems(universityIds: Seq[String], itemType: SchemeMembershipItemType): Seq[SchemeMembershipItem] = {
		val items = attendanceMonitoringDao.findSchemeMembershipItems(universityIds, itemType)
		items.map{ item => {
			val user = userLookup.getUserByWarwickUniId(item.universityId)
			SchemeMembershipItem(
				item.itemType,
				item.firstName,
				item.lastName,
				item.universityId,
				item.userId,
				membersHelper.findBy(user)
			)
		}}
	}

	def findPoints(
		department: Department,
		academicYear: AcademicYear,
		schemes: Seq[AttendanceMonitoringScheme],
		types: Seq[AttendanceMonitoringPointType],
		styles: Seq[AttendanceMonitoringPointStyle]
	): Seq[AttendanceMonitoringPoint] = {
		attendanceMonitoringDao.findPoints(department, academicYear, schemes, types, styles)
	}

	def findOldPoints(
		department: Department,
		academicYear: AcademicYear,
		sets: Seq[MonitoringPointSet],
		types: Seq[AttendanceMonitoringPointType]
	): Seq[MonitoringPoint] = {
		attendanceMonitoringDao.findOldPoints(department, academicYear, sets, types.map {
			case AttendanceMonitoringPointType.Standard => null
			case AttendanceMonitoringPointType.Meeting => MonitoringPointType.Meeting
			case AttendanceMonitoringPointType.SmallGroup => MonitoringPointType.SmallGroup
			case AttendanceMonitoringPointType.AssignmentSubmission => MonitoringPointType.AssignmentSubmission
		})
	}

	def listStudentsPoints(student: StudentMember, department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringPoint] = {
		student.mostSignificantCourseDetails.fold(Seq[AttendanceMonitoringPoint]())(scd => {
			val currentCourseStartDate = scd.beginDate
			val schemes = findSchemesForStudent(student, department, academicYear)
			schemes.flatMap(_.points.asScala).filter(p =>
				p.startDate.isAfter(currentCourseStartDate) || p.startDate.isEqual(currentCourseStartDate)
			)
		})
	}

	private def findSchemesForStudent(student: StudentMember, department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme] =
		membersHelper.findBy(MemberOrUser(student).asUser).filter(s => s.department == department && s.academicYear == academicYear)

	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint] = {
		attendanceMonitoringDao.getCheckpoints(points, student)
	}

	def getAttendanceNote(student: StudentMember, point: AttendanceMonitoringPoint): Option[AttendanceMonitoringNote] = {
		attendanceMonitoringDao.getAttendanceNote(student, point)
	}

	def countCheckpointsForPoint(point: AttendanceMonitoringPoint): Int =
		attendanceMonitoringDao.countCheckpointsForPoint(point)

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
	with AutowiringUserLookupComponent

