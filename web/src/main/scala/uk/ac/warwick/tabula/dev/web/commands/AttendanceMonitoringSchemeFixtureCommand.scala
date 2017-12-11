package uk.ac.warwick.tabula.dev.web.commands

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.{MeetingFormat, StudentMember, UserGroup}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringProfileServiceComponent, ModuleAndDepartmentServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

object AttendanceMonitoringSchemeFixtureCommand {
	def apply() =
		new AttendanceMonitoringSchemeFixtureCommand()
			with ComposableCommand[AttendanceMonitoringScheme]
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringProfileServiceComponent
			with AutowiringTransactionalComponent
			with Daoisms
			with PubliclyVisiblePermissions
			with Unaudited

}

class AttendanceMonitoringSchemeFixtureCommand extends CommandInternal[AttendanceMonitoringScheme] {

	this: AttendanceMonitoringServiceComponent with ModuleAndDepartmentServiceComponent
		with ProfileServiceComponent with TransactionalComponent with SessionComponent =>

	var deptCode: String = _
	var academicYear: AcademicYear = _
	var pointCount: Int = _
	var warwickId: String = _

	def applyInternal(): AttendanceMonitoringScheme = transactional() {

		val amDao = Wire[AttendanceMonitoringDao]

		val department = moduleAndDepartmentService.getDepartmentByCode(deptCode).getOrElse(throw new IllegalArgumentException)

		for (scheme <- attendanceMonitoringService.listSchemes(department, academicYear)) {
			for (point <- scheme.points){
				for (checkpoint <- attendanceMonitoringService.getAllCheckpoints(point)){
					session.delete(checkpoint)
				}
			}
			// the points will also be deleted by the cascade
			session.delete(scheme)
		}

		for (total <- amDao.getCheckpointTotals(department, academicYear)) session.delete(total)


		val scheme = new AttendanceMonitoringScheme
		scheme.academicYear = academicYear
		scheme.department = department
		scheme.createdDate = DateTime.now
		scheme.updatedDate = DateTime.now
		scheme.pointStyle = AttendanceMonitoringPointStyle.Week
		scheme.members = UserGroup.ofUniversityIds
		scheme.members.addUserId(warwickId)
		scheme.members.staticUserIds = Seq(warwickId)

		scheme.points = {
			for (count <- 0 until pointCount) yield {
				val point = new AttendanceMonitoringPoint
				point.name = s"Point ${count+1}"
				point.createdDate = DateTime.now
				point.updatedDate = DateTime.now
				point.scheme = scheme
				point.pointType = AttendanceMonitoringPointType.Meeting
				point.meetingFormats = MeetingFormat.members.toSeq
				point.startWeek = count + 1
				point.endWeek = count + 1
				point.startDate = academicYear.weeks(count + 1).firstDay
				point.endDate = academicYear.weeks(count + 1).lastDay.plusDays(1)
				point
			}
		}.asJava

		profileService.getMemberByUniversityId(warwickId).foreach {
			case studentMember: StudentMember =>
				val checkpointTotal = new AttendanceMonitoringCheckpointTotal
				checkpointTotal.student = studentMember
				checkpointTotal.academicYear = academicYear
				checkpointTotal.unrecorded = pointCount
				checkpointTotal.updatedDate = new DateTime()
				checkpointTotal.department = moduleAndDepartmentService.getDepartmentByCode(deptCode).orNull
				session.saveOrUpdate(checkpointTotal)
		}

		attendanceMonitoringService.saveOrUpdate(scheme)

		scheme

	}

}
