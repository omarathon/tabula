package uk.ac.warwick.tabula.services

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.attendance._
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear}
import uk.ac.warwick.tabula.data.{SchemeMembershipItemType, AutowiringAttendanceMonitoringDaoComponent, AttendanceMonitoringDaoComponent}
import uk.ac.warwick.tabula.data.SchemeMembershipItem
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPointType
import uk.ac.warwick.tabula.commands.MemberOrUser
import collection.JavaConverters._
import org.joda.time.DateTime

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
	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], student: StudentMember, withFlush: Boolean = false): Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]
	def countCheckpointsForPoint(point: AttendanceMonitoringPoint): Int
	def getAttendanceNote(student: StudentMember, point: AttendanceMonitoringPoint): Option[AttendanceMonitoringNote]
	def getAttendanceNoteMap(student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringNote]
	def setAttendance(student: StudentMember, attendanceMap: Map[AttendanceMonitoringPoint, AttendanceState], user: CurrentUser): Seq[AttendanceMonitoringCheckpoint]
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
		!findNonReportedTerms(Seq(student), point.scheme.academicYear).contains(
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

	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], student: StudentMember, withFlush: Boolean = false): Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint] = {
		attendanceMonitoringDao.getCheckpoints(points, student, withFlush)
	}

	def countCheckpointsForPoint(point: AttendanceMonitoringPoint): Int =
		attendanceMonitoringDao.countCheckpointsForPoint(point)
	

	def getAttendanceNote(student: StudentMember, point: AttendanceMonitoringPoint): Option[AttendanceMonitoringNote] = {
		attendanceMonitoringDao.getAttendanceNote(student, point)
	}

	def getAttendanceNoteMap(student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringNote] = {
		attendanceMonitoringDao.getAttendanceNoteMap(student)
	}

	def setAttendance(student: StudentMember, attendanceMap: Map[AttendanceMonitoringPoint, AttendanceState], user: CurrentUser): Seq[AttendanceMonitoringCheckpoint] = {
		val existingCheckpoints = getCheckpoints(attendanceMap.keys.toSeq, student)
		val checkpointsToDelete: Seq[AttendanceMonitoringCheckpoint] = attendanceMap.filter(_._2 == null).map(_._1).map(existingCheckpoints.get).flatten.toSeq
		val checkpointsToUpdate: Seq[AttendanceMonitoringCheckpoint] = attendanceMap.filter(_._2 != null).flatMap{case(point, state) =>
			val checkpoint = existingCheckpoints.getOrElse(point, {
				val checkpoint = new AttendanceMonitoringCheckpoint
				checkpoint.student = student
				checkpoint.point = point
				checkpoint.autoCreated = false
				checkpoint
			})
			if (checkpoint.state != state) {
				checkpoint.state = state
				checkpoint.updatedBy = user.apparentId
				checkpoint.updatedDate = DateTime.now
				Option(checkpoint)
			} else {
				None
			}
		}.toSeq
		attendanceMonitoringDao.removeCheckpoints(checkpointsToDelete)
		attendanceMonitoringDao.saveOrUpdateCheckpoints(checkpointsToUpdate)

		if (!attendanceMap.keys.isEmpty) {
			val scheme = attendanceMap.keys.head.scheme
			updateCheckpointTotal(student, scheme.department, scheme.academicYear)
		}

		checkpointsToUpdate
	}

	def updateCheckpointTotal(student: StudentMember, department: Department, academicYear: AcademicYear): AttendanceMonitoringCheckpointTotal = {
		val points = listStudentsPoints(student, department, academicYear)
		val checkpointMap = getCheckpoints(points, student, withFlush = true)
		val allCheckpoints = checkpointMap.map(_._2)

		val unrecorded = points.diff(checkpointMap.keys.toSeq).size
		val missedUnauthorised = allCheckpoints.count(_.state == AttendanceState.MissedUnauthorised)
		val missedAuthorised = allCheckpoints.count(_.state == AttendanceState.MissedAuthorised)
		val attended = allCheckpoints.count(_.state == AttendanceState.Attended)

		val totals = attendanceMonitoringDao.getCheckpointTotal(student, department, academicYear).getOrElse {
			val total = new AttendanceMonitoringCheckpointTotal
			total.student = student
			total.department = department
			total.academicYear = academicYear
			total
		}

		totals.unrecorded = unrecorded
		totals.unauthorized = missedUnauthorised
		totals.authorized = missedAuthorised
		totals.attended = attended
		totals.updatedDate = DateTime.now
		attendanceMonitoringDao.saveOrUpdate(totals)
		totals
	}
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

