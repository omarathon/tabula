package uk.ac.warwick.tabula.commands.attendance.manage.old

import uk.ac.warwick.tabula.commands.attendance.old.GroupMonitoringPointsByTerm
import uk.ac.warwick.tabula.data.model.{Assignment, Module, MeetingFormat, StudentRelationshipType, Department}
import org.springframework.util.AutoPopulatingList
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointType, MonitoringPoint}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentServiceComponent, SmallGroupServiceComponent}

trait MonitoringPointState extends GroupMonitoringPointsByTerm with SmallGroupServiceComponent with ModuleAndDepartmentServiceComponent {
	val dept: Department
	var monitoringPoints = new AutoPopulatingList(classOf[MonitoringPoint])
	var name: String = _
	var validFromWeek: Int = 0
	var requiredFromWeek: Int = 0
	var academicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(new DateTime())

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

	def monitoringPointsByTerm = groupByTerm(monitoringPoints.asScala, academicYear)

	def moduleHasSmallGroups(module: Module) = smallGroupService.hasSmallGroups(module, academicYear)
	def moduleHasAssignments(module: Module) = moduleAndDepartmentService.hasAssignments(module)

	def copyTo(point: MonitoringPoint): MonitoringPoint = {
		point.name = this.name
		point.validFromWeek = this.validFromWeek
		point.requiredFromWeek = this.requiredFromWeek
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

	def copyFrom(pointIndex: Int) {
		copyFrom(monitoringPoints.get(pointIndex))
	}

	def copyFrom(point: MonitoringPoint) {
		this.name = point.name
		this.validFromWeek = point.validFromWeek
		this.requiredFromWeek = point.requiredFromWeek
		this.pointType = point.pointType
		this.pointType match {
			case MonitoringPointType.Meeting =>
				meetingRelationships.clear()
				meetingRelationships.addAll(point.meetingRelationships.asJava)
				meetingFormats.clear()
				meetingFormats.addAll(point.meetingFormats.asJava)
				meetingQuantity = point.meetingQuantity
			case MonitoringPointType.SmallGroup =>
				smallGroupEventModules.clear()
				smallGroupEventModules.addAll(point.smallGroupEventModules.asJava)
				smallGroupEventQuantity = point.smallGroupEventQuantity
				smallGroupEventQuantityAll = point.smallGroupEventQuantity == 0
				isAnySmallGroupEventModules = point.smallGroupEventModules.size == 0
			case MonitoringPointType.AssignmentSubmission =>
				isSpecificAssignments = point.assignmentSubmissionIsSpecificAssignments
				assignmentSubmissionQuantity = point.assignmentSubmissionQuantity
				assignmentSubmissionModules.clear()
				assignmentSubmissionModules.addAll(point.assignmentSubmissionModules.asJava)
				assignmentSubmissionAssignments.clear()
				assignmentSubmissionAssignments.addAll(point.assignmentSubmissionAssignments.asJava)
				isAssignmentSubmissionDisjunction = point.assignmentSubmissionIsDisjunction
			case _ =>
		}
	}
}
