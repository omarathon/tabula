package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.{Assignment, Module, MeetingFormat, StudentRelationshipType, Department}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringPoint, MonitoringPointType, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.helpers.LazyLists
import org.joda.time.{DateTime, LocalDate}
import collection.JavaConverters._
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent, AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek

object CreateAttendancePointCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new CreateAttendancePointCommandInternal(department, academicYear)
			with ComposableCommand[Seq[AttendanceMonitoringPoint]]
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with CreateAttendancePointValidation
			with CreateAttendancePointDescription
			with CreateAttendancePointPermissions
			with CreateAttendancePointCommandState
}


class CreateAttendancePointCommandInternal(val department: Department, val academicYear: AcademicYear) 
	extends CommandInternal[Seq[AttendanceMonitoringPoint]] {

	self: CreateAttendancePointCommandState with AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def applyInternal() = {
		schemes.asScala.map(scheme => {
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
		validateName(errors, name)

		pointStyle match {
			case AttendanceMonitoringPointStyle.Date =>
				validateDate(errors, startDate, academicYear, "startDate")
				validateDate(errors, endDate, academicYear, "endDate")
				if (startDate != null && endDate != null) {
					validateDates(errors, startDate, endDate)
					validateCanPointBeEditedByDate(errors, startDate, schemes.asScala.map{_.members.members}.flatten, academicYear)
					validateDuplicateForDate(errors, null, name, startDate, endDate, schemes.asScala)
				}
			case AttendanceMonitoringPointStyle.Week =>
				validateWeek(errors, startWeek, "startWeek")
				validateWeek(errors, endWeek, "endWeek")
				validateWeeks(errors, startWeek, endWeek)
				validateCanPointBeEditedByWeek(errors, startWeek, schemes.asScala.map{_.members.members}.flatten, academicYear)
				validateDuplicateForWeek(errors, null, name, startWeek, endWeek, schemes.asScala)
		}

		pointType match {
			case MonitoringPointType.Meeting =>
				validateTypeMeeting(
					errors,
					meetingRelationships.asScala,
					meetingFormats.asScala,
					meetingQuantity,
					department
				)
			case MonitoringPointType.SmallGroup =>
				validateTypeSmallGroup(
					errors,
					smallGroupEventModules,
					isAnySmallGroupEventModules,
					smallGroupEventQuantity
				)
			case MonitoringPointType.AssignmentSubmission =>
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
		d.attendanceMonitoringSchemes(schemes.asScala)
	}
}

trait CreateAttendancePointCommandState {

	self: TermServiceComponent =>

	def department: Department
	def academicYear: AcademicYear

	// Bind variables

	// Which schemes to add the point to
	var schemes: JList[AttendanceMonitoringScheme] = LazyLists.create()
	lazy val pointStyle: AttendanceMonitoringPointStyle = schemes.asScala.head.pointStyle

	// The point's properties
	var name: String = _
	var startWeek: Int = 0
	var endWeek: Int = 0
	var startDate: LocalDate = _
	var endDate: LocalDate = _

	var pointType: MonitoringPointType = _

	var meetingRelationships: JSet[StudentRelationshipType] = JHashSet()
	var meetingFormats: JSet[MeetingFormat] = JHashSet()
	meetingFormats.addAll(MeetingFormat.members.asJava)
	var meetingQuantity: Int = 1

	var smallGroupEventQuantity: JInteger = 1
	var smallGroupEventQuantityAll: Boolean = false
	var smallGroupEventModules: JSet[Module] = JHashSet()
	var isAnySmallGroupEventModules: Boolean = true

	var isSpecificAssignments: Boolean = true
	var assignmentSubmissionQuantity: JInteger = 1
	var assignmentSubmissionModules: JSet[Module] = JHashSet()
	var assignmentSubmissionAssignments: JSet[Assignment] = JHashSet()
	var isAssignmentSubmissionDisjunction: Boolean = false

	def copyTo(point: AttendanceMonitoringPoint): AttendanceMonitoringPoint = {
		point.name = this.name
		pointStyle match {
			case AttendanceMonitoringPointStyle.Date =>
				point.startDate = startDate
				point.endDate = endDate
			case AttendanceMonitoringPointStyle.Week =>
				val weeksForYear = termService.getAcademicWeeksForYear(point.scheme.academicYear.dateInTermOne).toMap
				point.startWeek = startWeek
				point.endWeek = endWeek
				point.startDate = weeksForYear(startWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate
				point.endDate = weeksForYear(endWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate
		}
		point.pointType = pointType
		pointType match {
			case MonitoringPointType.Meeting =>
				point.meetingRelationships = meetingRelationships.asScala.toSeq
				point.meetingFormats = meetingFormats.asScala.toSeq
				point.meetingQuantity = meetingQuantity
			case MonitoringPointType.SmallGroup =>
				point.smallGroupEventQuantity = smallGroupEventQuantityAll match {
					case true => 0
					case _ => smallGroupEventQuantity.toInt
				}
				point.smallGroupEventModules = isAnySmallGroupEventModules match {
					case true => Seq()
					case false => smallGroupEventModules match {
						case modules: JSet[Module] => modules.asScala.toSeq
						case _ => Seq()
					}
				}
			case MonitoringPointType.AssignmentSubmission =>
				point.assignmentSubmissionIsSpecificAssignments = isSpecificAssignments
				point.assignmentSubmissionQuantity = assignmentSubmissionQuantity.toInt
				point.assignmentSubmissionModules = assignmentSubmissionModules match {
					case modules: JSet[Module] => modules.asScala.toSeq
					case _ => Seq()
				}
				point.assignmentSubmissionAssignments = assignmentSubmissionAssignments match {
					case assignments: JSet[Assignment] => assignments.asScala.toSeq
					case _ => Seq()
				}
				point.assignmentSubmissionIsDisjunction = isAssignmentSubmissionDisjunction
			case _ =>
		}
		point
	}
}
