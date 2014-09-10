package uk.ac.warwick.tabula.attendance.commands.manage

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointStyle, AttendanceMonitoringPointType, AttendanceMonitoringScheme, MonitoringPoint, MonitoringPointType}
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object CreateNewAttendancePointsFromCopyCommand {
	def apply(
		department: Department,
		academicYear: AcademicYear,
		schemes: Seq[AttendanceMonitoringScheme]
	) =
		new CreateNewAttendancePointsFromCopyCommandInternal(department, academicYear, schemes)
			with ComposableCommand[Seq[AttendanceMonitoringPoint]]
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringProfileServiceComponent
			with CreateNewAttendancePointsFromCopyValidation
			with CreateNewAttendancePointsFromCopyDescription
			with CreateNewAttendancePointsFromCopyPermissions
			with CreateNewAttendancePointsFromCopyCommandState
			with SetsFindPointsResultOnCommandState
}


class CreateNewAttendancePointsFromCopyCommandInternal(val department: Department, val academicYear: AcademicYear, val schemes: Seq[AttendanceMonitoringScheme])
	extends CommandInternal[Seq[AttendanceMonitoringPoint]] with GetsPointsToCreate with TaskBenchmarking with UpdatesAttendanceMonitoringScheme {

	self: CreateNewAttendancePointsFromCopyCommandState with TermServiceComponent
		with AttendanceMonitoringServiceComponent with ProfileServiceComponent =>

	override def applyInternal() = {
		val points = getPoints(findPointsResult, schemes, pointStyle)
		points.foreach(attendanceMonitoringService.saveOrUpdate)

		afterUpdate(schemes)

		points
	}

}

trait CreateNewAttendancePointsFromCopyValidation extends SelfValidating with GetsPointsToCreate with AttendanceMonitoringPointValidation {

	self: CreateNewAttendancePointsFromCopyCommandState with TermServiceComponent with AttendanceMonitoringServiceComponent =>

	override def validate(errors: Errors) {
		val points = getPoints(findPointsResult, schemes, pointStyle)
		points.foreach(point => {
			validateSchemePointStyles(errors, pointStyle, schemes.toSeq)

			pointStyle match {
				case AttendanceMonitoringPointStyle.Date =>
					validateCanPointBeEditedByDate(errors, point.startDate, schemes.map{_.members.members}.flatten, academicYear, "")
					validateDuplicateForDate(errors, point.name, point.startDate, point.endDate, schemes, global = true)
				case AttendanceMonitoringPointStyle.Week =>
					validateCanPointBeEditedByWeek(errors, point.startWeek, schemes.map{_.members.members}.flatten, academicYear, "")
					validateDuplicateForWeek(errors, point.name, point.startWeek, point.endWeek, schemes, global = true)
			}
		})
	}

}

trait GetsPointsToCreate {

	self: TermServiceComponent =>

	def getPoints(
		findPointsResult: FindPointsResult,
		schemes: Seq[AttendanceMonitoringScheme],
		pointStyle: AttendanceMonitoringPointStyle
	): Seq[AttendanceMonitoringPoint] = {
		val oldPoints = findPointsResult.termGroupedOldPoints.flatMap(_._2).map(_.templatePoint).toSeq
		val weekPoints = findPointsResult.termGroupedPoints.flatMap(_._2).map(_.templatePoint).toSeq
		val datePoints = findPointsResult.monthGroupedPoints.flatMap(_._2).map(_.templatePoint).toSeq
		if (oldPoints.size > 0) {
			// Old points to new points
			schemes.flatMap { scheme =>
				oldPoints.map { oldPoint =>
					val newPoint = new AttendanceMonitoringPoint
					newPoint.scheme = scheme
					newPoint.createdDate = DateTime.now
					newPoint.updatedDate = DateTime.now
					copyFromOldPoint(oldPoint, newPoint)
					newPoint
				}
			}
		} else if (pointStyle == AttendanceMonitoringPointStyle.Week) {
			// Week points
			schemes.flatMap { scheme =>
				weekPoints.map { weekPoint =>
					val newPoint = weekPoint.cloneTo(scheme)
					newPoint.createdDate = DateTime.now
					newPoint.updatedDate = DateTime.now
					newPoint
				}
			}
		} else {
			// Date points
			schemes.flatMap { scheme =>
				datePoints.map { datePoint =>
					val newPoint = datePoint.cloneTo(scheme)
					newPoint.createdDate = DateTime.now
					newPoint.updatedDate = DateTime.now
					newPoint
				}
			}
		}
	}

	private def copyFromOldPoint(oldPoint: MonitoringPoint, newPoint: AttendanceMonitoringPoint): AttendanceMonitoringPoint = {
		newPoint.name = oldPoint.name
		val weeksForYear = termService.getAcademicWeeksForYear(newPoint.scheme.academicYear.dateInTermOne).toMap
		newPoint.startWeek = oldPoint.validFromWeek
		newPoint.endWeek = oldPoint.requiredFromWeek
		newPoint.startDate = weeksForYear(oldPoint.validFromWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate
		newPoint.endDate = weeksForYear(oldPoint.requiredFromWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate.plusDays(6)
		newPoint.pointType = oldPoint.pointType match {
			case null => AttendanceMonitoringPointType.Standard
			case MonitoringPointType.Meeting => AttendanceMonitoringPointType.Meeting
			case MonitoringPointType.SmallGroup => AttendanceMonitoringPointType.SmallGroup
			case MonitoringPointType.AssignmentSubmission => AttendanceMonitoringPointType.AssignmentSubmission
		}
		oldPoint.pointType match {
			case MonitoringPointType.Meeting =>
				newPoint.meetingRelationships = oldPoint.meetingRelationships.toSeq
				newPoint.meetingFormats = oldPoint.meetingFormats.toSeq
				newPoint.meetingQuantity = oldPoint.meetingQuantity
			case MonitoringPointType.SmallGroup =>
				newPoint.smallGroupEventQuantity = oldPoint.smallGroupEventQuantity
				newPoint.smallGroupEventModules = oldPoint.smallGroupEventModules
			case MonitoringPointType.AssignmentSubmission =>
				newPoint.assignmentSubmissionIsSpecificAssignments = oldPoint.assignmentSubmissionIsSpecificAssignments
				newPoint.assignmentSubmissionQuantity = oldPoint.assignmentSubmissionQuantity
				newPoint.assignmentSubmissionModules = oldPoint.assignmentSubmissionModules
				newPoint.assignmentSubmissionAssignments = oldPoint.assignmentSubmissionAssignments
				newPoint.assignmentSubmissionIsDisjunction = oldPoint.assignmentSubmissionIsDisjunction
			case _ =>
		}
		newPoint
	}
}

trait CreateNewAttendancePointsFromCopyPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: CreateNewAttendancePointsFromCopyCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}

}

trait CreateNewAttendancePointsFromCopyDescription extends Describable[Seq[AttendanceMonitoringPoint]] {

	self: CreateNewAttendancePointsFromCopyCommandState =>

	override lazy val eventName = "CreateNewAttendancePointsFromCopy"

	override def describe(d: Description) {
		d.attendanceMonitoringSchemes(schemes)
	}
}

trait CreateNewAttendancePointsFromCopyCommandState extends FindPointsResultCommandState {
	def department: Department
	def academicYear: AcademicYear
	def schemes: Seq[AttendanceMonitoringScheme]
	lazy val pointStyle: AttendanceMonitoringPointStyle = schemes.head.pointStyle
}
