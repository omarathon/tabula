package uk.ac.warwick.tabula.services



import scala.collection.JavaConverters.asScalaBufferConverter

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{AutowiringMonitoringPointDaoComponent, MonitoringPointDaoComponent }
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpointState, MonitoringPointSet, MonitoringPointSetTemplate, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, Route, StudentMember}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import org.joda.time.DateTime

trait MonitoringPointServiceComponent {
	def monitoringPointService: MonitoringPointService
}

trait AutowiringMonitoringPointServiceComponent extends MonitoringPointServiceComponent {
	var monitoringPointService = Wire[MonitoringPointService]
}

trait MonitoringPointService {
	def saveOrUpdate(monitoringPoint : MonitoringPoint)
	def delete(monitoringPoint : MonitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint)
	def saveOrUpdate(set: MonitoringPointSet)
	def saveOrUpdate(template: MonitoringPointSetTemplate)
	def getPointById(id : String) : Option[MonitoringPoint]
	def getSetById(id : String) : Option[MonitoringPointSet]
	def findMonitoringPointSets(route: Route): Seq[MonitoringPointSet]
	def findMonitoringPointSets(route: Route, academicYear: AcademicYear): Seq[MonitoringPointSet]
	def findMonitoringPointSet(route: Route, year: Option[Int]): Option[MonitoringPointSet]
	def findMonitoringPointSet(route: Route, academicYear: AcademicYear, year: Option[Int]): Option[MonitoringPointSet]
	def getCheckpointsBySCD(monitoringPoint : MonitoringPoint) : Seq[(StudentCourseDetails, MonitoringCheckpoint)]
	def listTemplates : Seq[MonitoringPointSetTemplate]
	def getTemplateById(id: String) : Option[MonitoringPointSetTemplate]
	def deleteTemplate(template: MonitoringPointSetTemplate)
	def countCheckpointsForPoint(point: MonitoringPoint): Int
	def getChecked(members: Seq[StudentMember], set: MonitoringPointSet): Map[StudentMember, Map[MonitoringPoint, Option[MonitoringCheckpointState]]]
	def deleteCheckpoint(scjCode: String, point: MonitoringPoint): Unit
	def saveOrUpdateCheckpoint(
		studentCourseDetails: StudentCourseDetails,
		point: MonitoringPoint,
		state: MonitoringCheckpointState,
		user: CurrentUser
	) : MonitoringCheckpoint
}


abstract class AbstractMonitoringPointService extends MonitoringPointService {
	self: MonitoringPointDaoComponent =>

	def saveOrUpdate(monitoringPoint: MonitoringPoint) = monitoringPointDao.saveOrUpdate(monitoringPoint)
	def delete(monitoringPoint: MonitoringPoint) = monitoringPointDao.delete(monitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint) = monitoringPointDao.saveOrUpdate(monitoringCheckpoint)
	def saveOrUpdate(set: MonitoringPointSet) = monitoringPointDao.saveOrUpdate(set)
	def saveOrUpdate(template: MonitoringPointSetTemplate) = monitoringPointDao.saveOrUpdate(template)
	def getPointById(id: String): Option[MonitoringPoint] = monitoringPointDao.getPointById(id)
	def getSetById(id: String): Option[MonitoringPointSet] = monitoringPointDao.getSetById(id)
	def findMonitoringPointSets(route: Route): Seq[MonitoringPointSet] = monitoringPointDao.findMonitoringPointSets(route)
	def findMonitoringPointSets(route: Route, academicYear: AcademicYear): Seq[MonitoringPointSet] =
		monitoringPointDao.findMonitoringPointSets(route, academicYear)
	def findMonitoringPointSet(route: Route, year: Option[Int]) = monitoringPointDao.findMonitoringPointSet(route, year)
	def findMonitoringPointSet(route: Route, academicYear: AcademicYear, year: Option[Int]) =
		monitoringPointDao.findMonitoringPointSet(route, academicYear, year)
	
	def getCheckpointsBySCD(monitoringPoint: MonitoringPoint): Seq[(StudentCourseDetails, MonitoringCheckpoint)] =
		monitoringPointDao.getCheckpointsBySCD(monitoringPoint)

	def listTemplates = monitoringPointDao.listTemplates

	def getTemplateById(id: String): Option[MonitoringPointSetTemplate] = monitoringPointDao.getTemplateById(id)

	def deleteTemplate(template: MonitoringPointSetTemplate) = monitoringPointDao.deleteTemplate(template)

	def countCheckpointsForPoint(point: MonitoringPoint) = monitoringPointDao.countCheckpointsForPoint(point)

	def getChecked(members: Seq[StudentMember], set: MonitoringPointSet): Map[StudentMember, Map[MonitoringPoint, Option[MonitoringCheckpointState]]] =
		members.map(member =>
			member ->
			set.points.asScala.map(point =>
				(point, monitoringPointDao.getCheckpoint(point, member) match {
					case Some(c: MonitoringCheckpoint) => Option(c.state)
					case None => None
				})
			).toMap
		).toMap

	def deleteCheckpoint(scjCode: String, point: MonitoringPoint): Unit = {
		monitoringPointDao.getCheckpoint(point, scjCode) match {
			case None => // already gone
			case Some(checkpoint) => monitoringPointDao.deleteCheckpoint(checkpoint)
		}
	}

	def saveOrUpdateCheckpoint(
		studentCourseDetails: StudentCourseDetails,
		point: MonitoringPoint,
		state: MonitoringCheckpointState,
		user: CurrentUser
	) : MonitoringCheckpoint = {
		val checkpoint = monitoringPointDao.getCheckpoint(point, studentCourseDetails.student).getOrElse({
			val newCheckpoint = new MonitoringCheckpoint
			newCheckpoint.point = point
			newCheckpoint.studentCourseDetail = studentCourseDetails
			newCheckpoint
		})
		checkpoint.state = state
		checkpoint.updatedBy = user.apparentId
		checkpoint.updatedDate = DateTime.now
		monitoringPointDao.saveOrUpdate(checkpoint)
		checkpoint
	}
}

@Service("monitoringPointService")
class MonitoringPointServiceImpl
	extends AbstractMonitoringPointService
	with AutowiringMonitoringPointDaoComponent