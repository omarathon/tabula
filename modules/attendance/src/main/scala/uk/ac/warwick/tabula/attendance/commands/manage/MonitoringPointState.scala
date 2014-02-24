package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.data.model.{Module, MeetingFormat, StudentRelationshipType, Department}
import org.springframework.util.AutoPopulatingList
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointType, MonitoringPoint}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.attendance.commands.GroupMonitoringPointsByTerm

trait MonitoringPointState extends GroupMonitoringPointsByTerm {
	val dept: Department
	var monitoringPoints = new AutoPopulatingList(classOf[MonitoringPoint])
	var name: String = _
	var validFromWeek: Int = 0
	var requiredFromWeek: Int = 0

	var pointType: MonitoringPointType = _

	var meetingRelationships: JSet[StudentRelationshipType] = JHashSet()
	var meetingFormats: JSet[MeetingFormat] = JHashSet()
	meetingFormats.addAll(MeetingFormat.members.asJava)
	var meetingQuantity: Int = 1

	var smallGroupEventQuantity: JInteger = 0
	var smallGroupEventModules: JSet[Module] = JHashSet()

	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime())
	def monitoringPointsByTerm = groupByTerm(monitoringPoints.asScala, academicYear)
	def meetingRelationshipsStrings = meetingRelationships.asScala.map(_.urlPart)
	val allMeetingFormats = MeetingFormat.members
	def meetingFormatsStrings = meetingFormats.asScala.map(_.description)

	def copyTo(point: MonitoringPoint): MonitoringPoint = {
		point.name = this.name
		point.validFromWeek = this.validFromWeek
		point.requiredFromWeek = this.requiredFromWeek
		point.pointType = pointType
		pointType match {
			case MonitoringPointType.Meeting => {
				point.meetingRelationships = meetingRelationships.asScala.toSeq
				point.meetingFormats = meetingFormats.asScala.toSeq
				point.meetingQuantity = meetingQuantity
			}
			case MonitoringPointType.SmallGroup => {
				point.smallGroupEventQuantity = smallGroupEventQuantity
				point.smallGroupEventModules = smallGroupEventModules.asScala.toSeq
			}
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
			case MonitoringPointType.Meeting => {
				meetingRelationships.clear()
				meetingRelationships.addAll(point.meetingRelationships.asJava)
				meetingFormats.clear()
				meetingFormats.addAll(point.meetingFormats.asJava)
				meetingQuantity = point.meetingQuantity
			}
			case MonitoringPointType.SmallGroup => {
				smallGroupEventModules.clear()
				smallGroupEventModules.addAll(point.smallGroupEventModules.asJava)
				smallGroupEventQuantity = point.smallGroupEventQuantity
			}
			case _ =>
		}
	}
}
