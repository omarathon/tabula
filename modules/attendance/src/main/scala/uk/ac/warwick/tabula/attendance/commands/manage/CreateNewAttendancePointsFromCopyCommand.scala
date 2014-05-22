package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointType, MonitoringPointType, MonitoringPoint, AttendanceMonitoringPointStyle, AttendanceMonitoringScheme, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent, TermServiceComponent}

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
			with CreateNewAttendancePointsFromCopyValidation
			with CreateNewAttendancePointsFromCopyDescription
			with CreateNewAttendancePointsFromCopyPermissions
			with CreateNewAttendancePointsFromCopyCommandState
			with SetsFindResultOnCreateNewAttendancePointsFromCopyCommand
}


class CreateNewAttendancePointsFromCopyCommandInternal(val department: Department, val academicYear: AcademicYear, val schemes: Seq[AttendanceMonitoringScheme])
	extends CommandInternal[Seq[AttendanceMonitoringPoint]] {

	self: CreateNewAttendancePointsFromCopyCommandState with TermServiceComponent with AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
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
					attendanceMonitoringService.saveOrUpdate(newPoint)
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
					attendanceMonitoringService.saveOrUpdate(newPoint)
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
					attendanceMonitoringService.saveOrUpdate(newPoint)
					newPoint
				}
			}
		}
	}

	private def copyFromOldPoint(oldPoint: MonitoringPoint, newPoint: AttendanceMonitoringPoint): AttendanceMonitoringPoint = {
		oldPoint.name = oldPoint.name
		val weeksForYear = termService.getAcademicWeeksForYear(newPoint.scheme.academicYear.dateInTermOne).toMap
		newPoint.startWeek = oldPoint.validFromWeek
		newPoint.endWeek = oldPoint.requiredFromWeek
		newPoint.startDate = weeksForYear(oldPoint.validFromWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate
		newPoint.endDate = weeksForYear(oldPoint.requiredFromWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate
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

trait CreateNewAttendancePointsFromCopyValidation extends SelfValidating {

	self: CreateNewAttendancePointsFromCopyCommandState =>

	override def validate(errors: Errors) {
		// TODO some validation
	}

}

trait SetsFindResultOnCreateNewAttendancePointsFromCopyCommand {

	self: CreateNewAttendancePointsFromCopyCommandState =>

	def setFindResult(result: FindPointsResult) {
		findPointsResult = result
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

trait CreateNewAttendancePointsFromCopyCommandState {
	def department: Department
	def academicYear: AcademicYear
	def schemes: Seq[AttendanceMonitoringScheme]
	lazy val pointStyle: AttendanceMonitoringPointStyle = schemes.head.pointStyle

	var findPointsResult: FindPointsResult = _
}
