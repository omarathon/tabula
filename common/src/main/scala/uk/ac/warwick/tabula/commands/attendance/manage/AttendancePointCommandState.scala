package uk.ac.warwick.tabula.commands.attendance.manage

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointStyle, AttendanceMonitoringPointType}
import uk.ac.warwick.tabula.data.model.{Assignment, MeetingFormat, Module, StudentRelationshipType}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentServiceComponent, SmallGroupServiceComponent}

import scala.collection.JavaConverters._

trait AttendancePointCommandState {

	self: SmallGroupServiceComponent with ModuleAndDepartmentServiceComponent =>

	def pointStyle: AttendanceMonitoringPointStyle
	def academicYear: AcademicYear

	// Bind variables

	// The point's properties
	var name: String = _
	var startWeek: Int = 1
	var endWeek: Int = 1
	var startDate: LocalDate = _
	var endDate: LocalDate = _

	var pointType: AttendanceMonitoringPointType = AttendanceMonitoringPointType.Standard

	var meetingRelationships: JSet[StudentRelationshipType] = JHashSet()
	var meetingFormats: JSet[MeetingFormat] = JHashSet()
	meetingFormats.addAll(MeetingFormat.members.asJava)
	var meetingQuantity: Int = 1

	var smallGroupEventQuantity: JInteger = 1
	var smallGroupEventQuantityAll: Boolean = false
	var smallGroupEventModules: JSet[Module] = JHashSet()
	var isAnySmallGroupEventModules: Boolean = true

	var assignmentSubmissionType: String = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Any
	var assignmentSubmissionTypeAnyQuantity: JInteger = 1
	var assignmentSubmissionTypeModulesQuantity: JInteger = 1
	var assignmentSubmissionModules: JSet[Module] = JHashSet()
	var assignmentSubmissionAssignments: JSet[Assignment] = JHashSet()
	var isAssignmentSubmissionDisjunction: Boolean = false

	def copyTo(point: AttendanceMonitoringPoint): AttendanceMonitoringPoint = {
		point.name = name
		pointStyle match {
			case AttendanceMonitoringPointStyle.Date =>
				point.startDate = startDate
				point.endDate = endDate
			case AttendanceMonitoringPointStyle.Week =>
				val weeksForYear = point.scheme.academicYear.weeks
				point.startWeek = startWeek
				point.endWeek = endWeek
				point.startDate = weeksForYear(startWeek).firstDay
				point.endDate = weeksForYear(endWeek).lastDay
		}
		point.pointType = pointType
		pointType match {
			case AttendanceMonitoringPointType.Meeting =>
				point.meetingRelationships = meetingRelationships.asScala.toSeq
				point.meetingFormats = meetingFormats.asScala.toSeq
				point.meetingQuantity = meetingQuantity
			case AttendanceMonitoringPointType.SmallGroup =>
				point.smallGroupEventQuantity = if (smallGroupEventQuantityAll) 0 else smallGroupEventQuantity.toInt
				point.smallGroupEventModules = if (isAnySmallGroupEventModules) Nil else smallGroupEventModules match {
					case modules: JSet[Module] => modules.asScala.toSeq
					case _ => Seq()
				}
			case AttendanceMonitoringPointType.AssignmentSubmission =>
				point.assignmentSubmissionType = assignmentSubmissionType
				point.assignmentSubmissionTypeAnyQuantity = Option(assignmentSubmissionTypeAnyQuantity).getOrElse(JInteger(Option(0))).toInt
				point.assignmentSubmissionTypeModulesQuantity = Option(assignmentSubmissionTypeModulesQuantity).getOrElse(JInteger(Option(0))).toInt
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

	def copyFrom(point: AttendanceMonitoringPoint): AttendanceMonitoringPoint = {
		name = point.name
		pointStyle match {
			case AttendanceMonitoringPointStyle.Date =>
				startDate = point.startDate
				endDate = point.endDate
			case AttendanceMonitoringPointStyle.Week =>
				startWeek = point.startWeek
				endWeek = point.endWeek
		}
		pointType = point.pointType
		pointType match {
			case AttendanceMonitoringPointType.Meeting =>
				meetingRelationships.clear()
				meetingRelationships.addAll(point.meetingRelationships.asJava)
				meetingFormats.clear()
				meetingFormats.addAll(point.meetingFormats.asJava)
				meetingQuantity = point.meetingQuantity
			case AttendanceMonitoringPointType.SmallGroup =>
				smallGroupEventModules.clear()
				smallGroupEventModules.addAll(point.smallGroupEventModules.asJava)
				smallGroupEventQuantity = point.smallGroupEventQuantity
				smallGroupEventQuantityAll = point.smallGroupEventQuantity == 0
				isAnySmallGroupEventModules = point.smallGroupEventModules.isEmpty
			case AttendanceMonitoringPointType.AssignmentSubmission =>
				assignmentSubmissionType = point.assignmentSubmissionType
				assignmentSubmissionTypeAnyQuantity = point.assignmentSubmissionTypeAnyQuantity
				assignmentSubmissionTypeModulesQuantity = point.assignmentSubmissionTypeModulesQuantity
				assignmentSubmissionModules.clear()
				assignmentSubmissionModules.addAll(point.assignmentSubmissionModules.asJava)
				assignmentSubmissionAssignments.clear()
				assignmentSubmissionAssignments.addAll(point.assignmentSubmissionAssignments.asJava)
				isAssignmentSubmissionDisjunction = point.assignmentSubmissionIsDisjunction
			case _ =>
		}
		point
	}

	def moduleHasSmallGroups(module: Module): Boolean = smallGroupService.hasSmallGroups(module, academicYear)
	def moduleHasAssignments(module: Module): Boolean = moduleAndDepartmentService.hasAssignments(module)

}
