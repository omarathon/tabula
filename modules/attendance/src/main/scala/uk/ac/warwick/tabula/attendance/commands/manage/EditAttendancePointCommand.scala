package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringScheme, AttendanceMonitoringPointType, AttendanceMonitoringPointStyle, AttendanceMonitoringPoint}
import org.joda.time.DateTime
import collection.JavaConverters._
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent, AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}

object EditAttendancePointCommand {
	def apply(department: Department, academicYear: AcademicYear, templatePoint: AttendanceMonitoringPoint, findPointsResult: FindPointsResult) =
		new EditAttendancePointCommandInternal(department, academicYear, templatePoint, findPointsResult)
			with ComposableCommand[Seq[AttendanceMonitoringPoint]]
			with PopulatesEditAttendancePointCommand
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with EditAttendancePointValidation
			with EditAttendancePointDescription
			with EditAttendancePointPermissions
			with EditAttendancePointCommandState
}


class EditAttendancePointCommandInternal(
	val department: Department,
	val academicYear: AcademicYear,
	val templatePoint: AttendanceMonitoringPoint,
	val findPointsResult: FindPointsResult
) extends CommandInternal[Seq[AttendanceMonitoringPoint]] {

	self: EditAttendancePointCommandState with AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def applyInternal() = {
		pointsToEdit.map(point => {
			val point = new AttendanceMonitoringPoint
			point.updatedDate = DateTime.now
			point.createdDate = templatePoint.createdDate
			point.scheme = templatePoint.scheme
			copyTo(point)
			attendanceMonitoringService.saveOrUpdate(point)
			point
		})
	}

}

trait PopulatesEditAttendancePointCommand extends PopulateOnForm {

	self: EditAttendancePointCommandState =>

	override def populate() = {
		copyFrom(templatePoint)
	}
}

trait EditAttendancePointValidation extends SelfValidating with AttendanceMonitoringPointValidation {

	self: EditAttendancePointCommandState with TermServiceComponent with AttendanceMonitoringServiceComponent =>

	override def validate(errors: Errors) {
		val points = pointsToEdit
		val schemes = schemesToEdit
		validateSchemePointStyles(errors, pointStyle, schemes)

		validateName(errors, name)

		pointStyle match {
			case AttendanceMonitoringPointStyle.Date =>
				validateDate(errors, startDate, academicYear, "startDate")
				validateDate(errors, endDate, academicYear, "endDate")
				if (startDate != null && endDate != null) {
					validateDates(errors, startDate, endDate)
					validateCanPointBeEditedByDate(errors, startDate, schemes.map{_.members.members}.flatten, academicYear)
					points.exists(p => validateDuplicateForDateForEdit(errors, name, startDate, endDate, p))
				}
			case AttendanceMonitoringPointStyle.Week =>
				validateWeek(errors, startWeek, "startWeek")
				validateWeek(errors, endWeek, "endWeek")
				validateWeeks(errors, startWeek, endWeek)
				validateCanPointBeEditedByWeek(errors, startWeek, schemes.map{_.members.members}.flatten, academicYear)
				points.exists(p => validateDuplicateForWeekForEdit(errors, name, startWeek, endWeek, p))
		}

		pointType match {
			case AttendanceMonitoringPointType.Meeting =>
				validateTypeMeeting(
					errors,
					meetingRelationships.asScala,
					meetingFormats.asScala,
					meetingQuantity,
					department
				)
			case AttendanceMonitoringPointType.SmallGroup =>
				validateTypeSmallGroup(
					errors,
					smallGroupEventModules,
					isAnySmallGroupEventModules,
					smallGroupEventQuantity
				)
			case AttendanceMonitoringPointType.AssignmentSubmission =>
				validateTypeAssignmentSubmission(
					errors,
					isSpecificAssignments,
					assignmentSubmissionQuantity,
					assignmentSubmissionModules,
					assignmentSubmissionAssignments
				)
			case _ =>
		}
	}

}

trait EditAttendancePointPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: EditAttendancePointCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}

}

trait EditAttendancePointDescription extends Describable[Seq[AttendanceMonitoringPoint]] {

	self: EditAttendancePointCommandState =>

	override lazy val eventName = "EditAttendancePoint"

	override def describe(d: Description) {
		d.attendanceMonitoringPoints(pointsToEdit)
	}
}

trait EditAttendancePointCommandState extends AttendancePointCommandState {

	self: TermServiceComponent =>

	def department: Department
	def academicYear: AcademicYear
	def templatePoint: AttendanceMonitoringPoint
	def findPointsResult: FindPointsResult
	lazy val pointStyle: AttendanceMonitoringPointStyle = templatePoint.scheme.pointStyle

	def pointsToEdit: Seq[AttendanceMonitoringPoint] = (pointStyle match {
		case AttendanceMonitoringPointStyle.Week => findPointsResult.termGroupedPoints
		case AttendanceMonitoringPointStyle.Date => findPointsResult.monthGroupedPoints
	}).flatMap(_._2)
		.find(p => p.templatePoint.id == templatePoint.id)
		.getOrElse(throw new IllegalArgumentException)
		.points

	def schemesToEdit: Seq[AttendanceMonitoringScheme] = pointsToEdit.map(_.scheme)
}
