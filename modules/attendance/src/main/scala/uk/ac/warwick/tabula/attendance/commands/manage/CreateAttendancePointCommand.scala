package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointType, AttendanceMonitoringPointStyle, AttendanceMonitoringPoint, AttendanceMonitoringScheme}
import org.joda.time.DateTime
import collection.JavaConverters._
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent, AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}

object CreateAttendancePointCommand {
	def apply(department: Department, academicYear: AcademicYear, schemes: Seq[AttendanceMonitoringScheme]) =
		new CreateAttendancePointCommandInternal(department, academicYear, schemes)
			with ComposableCommand[Seq[AttendanceMonitoringPoint]]
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with CreateAttendancePointValidation
			with CreateAttendancePointDescription
			with CreateAttendancePointPermissions
			with CreateAttendancePointCommandState
}


class CreateAttendancePointCommandInternal(val department: Department, val academicYear: AcademicYear, val schemes: Seq[AttendanceMonitoringScheme])
	extends CommandInternal[Seq[AttendanceMonitoringPoint]] {

	self: CreateAttendancePointCommandState with AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def applyInternal() = {
		schemes.map(scheme => {
			val point = new AttendanceMonitoringPoint
			point.scheme = scheme
			point.createdDate = DateTime.now
			point.updatedDate = DateTime.now
			copyTo(point)
			attendanceMonitoringService.saveOrUpdate(point)
			point
		})
	}

}

trait CreateAttendancePointValidation extends SelfValidating with AttendanceMonitoringPointValidation {

	self: CreateAttendancePointCommandState with TermServiceComponent with AttendanceMonitoringServiceComponent =>

	override def validate(errors: Errors) {
		validateSchemePointStyles(errors, pointStyle, schemes.toSeq)

		validateName(errors, name)

		pointStyle match {
			case AttendanceMonitoringPointStyle.Date =>
				validateDate(errors, startDate, academicYear, "startDate")
				validateDate(errors, endDate, academicYear, "endDate")
				if (startDate != null && endDate != null) {
					validateDates(errors, startDate, endDate)
					validateCanPointBeEditedByDate(errors, startDate, schemes.map{_.members.members}.flatten, academicYear)
					validateDuplicateForDate(errors, name, startDate, endDate, schemes)
				}
			case AttendanceMonitoringPointStyle.Week =>
				validateWeek(errors, startWeek, "startWeek")
				validateWeek(errors, endWeek, "endWeek")
				validateWeeks(errors, startWeek, endWeek)
				validateCanPointBeEditedByWeek(errors, startWeek, schemes.map{_.members.members}.flatten, academicYear)
				validateDuplicateForWeek(errors, name, startWeek, endWeek, schemes)
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

trait CreateAttendancePointPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: CreateAttendancePointCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}

}

trait CreateAttendancePointDescription extends Describable[Seq[AttendanceMonitoringPoint]] {

	self: CreateAttendancePointCommandState =>

	override lazy val eventName = "CreateAttendancePoint"

	override def describe(d: Description) {
		d.attendanceMonitoringSchemes(schemes)
	}
}

trait CreateAttendancePointCommandState extends AttendancePointCommandState {

	self: TermServiceComponent =>

	def department: Department
	def academicYear: AcademicYear
	def schemes: Seq[AttendanceMonitoringScheme]
	lazy val pointStyle: AttendanceMonitoringPointStyle = schemes.head.pointStyle
}
