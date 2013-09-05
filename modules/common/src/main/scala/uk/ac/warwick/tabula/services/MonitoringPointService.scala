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
	def list(page: Int) : Seq[MonitoringPoint]
	def getCheckedStudents(monitoringPoint : MonitoringPoint) : Seq[StudentMember]
	def updateCheckedStudents(monitoringPoint: MonitoringPoint, members: Seq[StudentMember], user: CurrentUser): Seq[MonitoringCheckpoint]
	def listTemplates : Seq[MonitoringPointSetTemplate]
	def getTemplateById(id: String) : Option[MonitoringPointSetTemplate]
	def deleteTemplate(template: MonitoringPointSetTemplate)
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
	def list(page: Int) : Seq[MonitoringPoint] = monitoringPointDao.list(page)
	def getCheckedStudents(monitoringPoint: MonitoringPoint): Seq[StudentMember] = monitoringPointDao.getStudentsChecked(monitoringPoint)

	def updateCheckedStudents(monitoringPoint: MonitoringPoint, members: Seq[StudentMember], user: CurrentUser): Seq[MonitoringCheckpoint] = {
		clearCheckedStudents(monitoringPoint)

	 val updatedCheckpoints  = members.map(member => {
		 val checkpoint = monitoringPointDao.getCheckpoint(monitoringPoint, member) getOrElse {
			 val newCheckpoint = new MonitoringCheckpoint()
			 newCheckpoint.studentCourseDetail = member.studentCourseDetails.asScala.filter(
				 scd => scd.route == monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet].route
			 ).head  //todo
			 newCheckpoint.point = monitoringPoint
			 newCheckpoint.createdBy = user.apparentId
			 newCheckpoint.createdDate = DateTime.now
			 newCheckpoint
		 }
		 checkpoint.checked = true
		 saveOrUpdate(checkpoint)
		 checkpoint
	 })

		updatedCheckpoints
	}

	private def clearCheckedStudents(monitoringPoint: MonitoringPoint) = {
		val checkpoints = monitoringPointDao.getCheckpoints(monitoringPoint: MonitoringPoint)
		for (checkpoint <- checkpoints) {
			checkpoint.checked = false
			saveOrUpdate(checkpoint)
		}
	}

	def listTemplates = monitoringPointDao.listTemplates

	def getTemplateById(id: String): Option[MonitoringPointSetTemplate] = monitoringPointDao.getTemplateById(id)

	def deleteTemplate(template: MonitoringPointSetTemplate) = monitoringPointDao.deleteTemplate(template)
}

@Service("monitoringPointService")
class MonitoringPointServiceImpl
	extends AbstractMonitoringPointService
	with AutowiringMonitoringPointDaoComponent