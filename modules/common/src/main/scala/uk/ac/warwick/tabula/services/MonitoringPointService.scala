package uk.ac.warwick.tabula.services



import scala.collection.JavaConverters.asScalaBufferConverter

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{AutowiringMonitoringPointDaoComponent, MonitoringPointDaoComponent }
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPointSetTemplate, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.CurrentUser
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
	def getStudents(monitoringPoint : MonitoringPoint) : Seq[(StudentMember, Boolean)]
	def updateStudents(monitoringPoint: MonitoringPoint, changedStudentMembers: Seq[(StudentMember, Boolean)], user: CurrentUser): Seq[MonitoringCheckpoint]
	def listTemplates : Seq[MonitoringPointSetTemplate]
	def getTemplateById(id: String) : Option[MonitoringPointSetTemplate]
	def deleteTemplate(template: MonitoringPointSetTemplate)
	def countCheckpointsForPoint(point: MonitoringPoint): Int
	def getCheckedForWeek(members: Seq[StudentMember], set: MonitoringPointSet, week: Int): Map[StudentMember, Map[MonitoringPoint, Option[Boolean]]]
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
	def getStudents(monitoringPoint: MonitoringPoint): Seq[(StudentMember, Boolean)] = monitoringPointDao.getStudents(monitoringPoint)

	def updateStudents(monitoringPoint: MonitoringPoint, changedStudentMembers: Seq[(StudentMember, Boolean)], user: CurrentUser): Seq[MonitoringCheckpoint] = {
		val checkpointsChanged = changedStudentMembers.map(student => {
			val checkpoint = monitoringPointDao.getCheckpoint(monitoringPoint, student._1) getOrElse {
				createNewCheckpoint(monitoringPoint, student._1, user)
			}
			checkpoint.checked = student._2
			saveOrUpdate(checkpoint)
			checkpoint
		})

		checkpointsChanged
	}

	private def createNewCheckpoint(monitoringPoint: MonitoringPoint, student: StudentMember, user: CurrentUser): MonitoringCheckpoint =  {
		val newCheckpoint = new MonitoringCheckpoint()
		newCheckpoint.studentCourseDetail = student.studentCourseDetails.asScala.filter(
			scd => scd.route == monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet].route
		).head
		newCheckpoint.point = monitoringPoint
		newCheckpoint.createdBy = user.apparentId
		newCheckpoint.createdDate = DateTime.now
		newCheckpoint
	}


	def listTemplates = monitoringPointDao.listTemplates

	def getTemplateById(id: String): Option[MonitoringPointSetTemplate] = monitoringPointDao.getTemplateById(id)

	def deleteTemplate(template: MonitoringPointSetTemplate) = monitoringPointDao.deleteTemplate(template)

	def countCheckpointsForPoint(point: MonitoringPoint) = monitoringPointDao.countCheckpointsForPoint(point)

	def getCheckedForWeek(members: Seq[StudentMember], set: MonitoringPointSet, week: Int): Map[StudentMember, Map[MonitoringPoint, Option[Boolean]]] =
		members.map(member =>
			member ->
			set.points.asScala.map(point =>
				(point, monitoringPointDao.getCheckpoint(point, member) match {
					case Some(c: MonitoringCheckpoint) => Option(c.checked)
					case None =>
						if (week <= point.week)
							None
						else
							Option(point.defaultValue)
				})
			).toMap
		).toMap
}

@Service("monitoringPointService")
class MonitoringPointServiceImpl
	extends AbstractMonitoringPointService
	with AutowiringMonitoringPointDaoComponent