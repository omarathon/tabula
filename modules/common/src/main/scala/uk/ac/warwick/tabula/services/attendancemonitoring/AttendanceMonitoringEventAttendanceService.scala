package uk.ac.warwick.tabula.services.attendancemonitoring

import org.joda.time.{DateTime, LocalDate}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, SmallGroupEventAttendance}
import uk.ac.warwick.tabula.data.model.{Module, StudentMember}
import uk.ac.warwick.tabula.services._

trait AttendanceMonitoringEventAttendanceServiceComponent {
	def attendanceMonitoringEventAttendanceService: AttendanceMonitoringEventAttendanceService
}

trait AutowiringAttendanceMonitoringEventAttendanceServiceComponent extends AttendanceMonitoringEventAttendanceServiceComponent {
	val attendanceMonitoringEventAttendanceService = Wire[AttendanceMonitoringEventAttendanceService]
}

trait AttendanceMonitoringEventAttendanceService {
	def getCheckpoints(attendances: Seq[SmallGroupEventAttendance]): Seq[AttendanceMonitoringCheckpoint]
	def updateCheckpoints(attendances: Seq[SmallGroupEventAttendance]): Seq[AttendanceMonitoringCheckpoint]
}

abstract class AbstractAttendanceMonitoringEventAttendanceService extends AttendanceMonitoringEventAttendanceService {

	self: ProfileServiceComponent with AttendanceMonitoringServiceComponent with SmallGroupServiceComponent with TermServiceComponent =>

	def getCheckpoints(attendances: Seq[SmallGroupEventAttendance]): Seq[AttendanceMonitoringCheckpoint] = {
		attendances.filter(a => a.state == AttendanceState.Attended && a.occurrence.event.day != null).flatMap(attendance => {
			profileService.getMemberByUniversityId(attendance.universityId).flatMap{
				case studentMember: StudentMember =>
					val relevantPoints = getRelevantPoints(
						attendanceMonitoringService.listStudentsPoints(studentMember, None, attendance.occurrence.event.group.groupSet.academicYear),
						attendance,
						studentMember
					)
					val checkpoints = relevantPoints.filter(point => checkQuantity(point, attendance, studentMember)).map(point => {
						val checkpoint = new AttendanceMonitoringCheckpoint
						checkpoint.autoCreated = true
						checkpoint.point = point
						checkpoint.attendanceMonitoringService = attendanceMonitoringService
						checkpoint.student = studentMember
						checkpoint.updatedBy = attendance.updatedBy
						checkpoint.updatedDate = DateTime.now
						checkpoint.state = AttendanceState.Attended
						checkpoint
					})
					Option(checkpoints)
				case _ => None
			}
		}).flatten
	}

	def updateCheckpoints(attendances: Seq[SmallGroupEventAttendance]): Seq[AttendanceMonitoringCheckpoint] = {
		getCheckpoints(attendances).flatMap(checkpoint => {
			attendanceMonitoringService.setAttendance(checkpoint.student, Map(checkpoint.point -> checkpoint.state), checkpoint.updatedBy, autocreated = true)
		})
	}

	private def getRelevantPoints(points: Seq[AttendanceMonitoringPoint], attendance: SmallGroupEventAttendance, studentMember: StudentMember): Seq[AttendanceMonitoringPoint] = {
		val eventDateOption = attendance.occurrence.date
		eventDateOption.map(eventDate =>
			points.filter(point =>
				// Is it the correct type
				point.pointType == AttendanceMonitoringPointType.SmallGroup
					// Is the attendance inside the point's weeks
					&& point.isDateValidForPoint(eventDate)
					// Is the group's module valid
					&& (point.smallGroupEventModules.isEmpty || point.smallGroupEventModules.contains(attendance.occurrence.event.group.groupSet.module))
					// Is there no existing checkpoint
					&& attendanceMonitoringService.getCheckpoints(Seq(point), Seq(studentMember)).isEmpty
					// The student hasn't been sent to SITS for this point
					&& !attendanceMonitoringService.studentAlreadyReportedThisTerm(studentMember, point)
		)).getOrElse(Seq())
	}

	private def checkQuantity(point: AttendanceMonitoringPoint, attendance: SmallGroupEventAttendance, student: StudentMember): Boolean = {
		if (point.smallGroupEventQuantity == 1) {
			true
		}	else {
			val attendances = findAttendanceForDates(point.startDate, point.endDate, attendance.occurrence.event.group.groupSet.academicYear, student, point.smallGroupEventModules)
				.filterNot(a => a.occurrence == attendance.occurrence && a.universityId == attendance.universityId)
			point.smallGroupEventQuantity <= attendances.size + 1
		}
	}

	private def findAttendanceForDates(
		startDate: LocalDate,
		endDate: LocalDate,
		academicYear: AcademicYear,
		student: StudentMember,
		modules: Seq[Module]
	): Seq[SmallGroupEventAttendance] = {
		val startWeek = termService.getAcademicWeekForAcademicYear(startDate.toDateTimeAtStartOfDay, academicYear)
		val endWeek = termService.getAcademicWeekForAcademicYear(endDate.toDateTimeAtStartOfDay, academicYear)
		val weekAttendances = smallGroupService.findAttendanceForStudentInModulesInWeeks(student, startWeek, endWeek, modules)
		// weekAttendances may contain attendance before the startDate and after the endDate, so filter those out
		// don't need to filter if the startDate is a MOnday and the endDate is a Sunday, as that's the whole week
		if (startDate.getDayOfWeek == DayOfWeek.Monday.jodaDayOfWeek && endDate.getDayOfWeek == DayOfWeek.Sunday.jodaDayOfWeek) {
			weekAttendances
		} else {
			weekAttendances.filter(a =>
				a.occurrence.week > startWeek && a.occurrence.week < endWeek
				|| a.occurrence.week == startWeek && a.occurrence.event.day.jodaDayOfWeek >= startDate.getDayOfWeek
				|| a.occurrence.week == endWeek && a.occurrence.event.day.jodaDayOfWeek <= endDate.getDayOfWeek
			)
		}
	}


}

@Service("attendanceMonitoringEventAttendanceService")
class AttendanceMonitoringEventAttendanceServiceImpl
	extends AbstractAttendanceMonitoringEventAttendanceService
	with AutowiringAttendanceMonitoringServiceComponent
	with AutowiringProfileServiceComponent
	with AutowiringTermServiceComponent
	with AutowiringSmallGroupServiceComponent
