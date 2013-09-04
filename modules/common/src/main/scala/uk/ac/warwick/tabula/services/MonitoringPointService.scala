package uk.ac.warwick.tabula.services



import scala.collection.JavaConverters.asScalaBufferConverter

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{AutowiringMonitoringPointDaoComponent, MonitoringPointDaoComponent }
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.CurrentUser
import org.joda.time.{LocalTime, DateTime}

trait MonitoringPointServiceComponent {
	def monitoringPointService: MonitoringPointService
}

trait AutowiringMonitoringPointServiceComponent extends MonitoringPointServiceComponent {
	var monitoringPointService = Wire[MonitoringPointService]
}

trait MonitoringPointService {
	def saveOrUpdate(monitoringPoint : MonitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint)
	def getById(id : String) : Option[MonitoringPoint]
	def list(page: Int) : Seq[MonitoringPoint]
	def getCheckedStudents(monitoringPoint : MonitoringPoint) : Seq[StudentMember]
	def updateCheckedStudents(monitoringPoint: MonitoringPoint, members: Seq[StudentMember], user: CurrentUser): Seq[MonitoringCheckpoint]
}


abstract class AbstractMonitoringPointService extends MonitoringPointService {
	self: MonitoringPointDaoComponent =>

	def saveOrUpdate(monitoringPoint: MonitoringPoint) = monitoringPointDao.saveOrUpdate(monitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint) = monitoringPointDao.saveOrUpdate(monitoringCheckpoint)
	def getById(id: String): Option[MonitoringPoint] = monitoringPointDao.getById(id)
	def list(page: Int) : Seq[MonitoringPoint] = monitoringPointDao.list(page)
	def getCheckedStudents(monitoringPoint: MonitoringPoint): Seq[StudentMember] = monitoringPointDao.getStudentsChecked(monitoringPoint)

	def updateCheckedStudents(monitoringPoint: MonitoringPoint, members: Seq[StudentMember], user: CurrentUser): Seq[MonitoringCheckpoint] = {
		clearCheckedStudents(monitoringPoint)

	 val updatedCheckpoints  = members.map(member => {
		 val checkpoint = monitoringPointDao.getCheckpoint(monitoringPoint, member) getOrElse {
			 val newCheckpoint = new MonitoringCheckpoint()
			 newCheckpoint.studentCourseDetail = member.studentCourseDetails.asScala.filter(scd => scd.route == monitoringPoint.pointSet.route).head  //todo
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




}

@Service("monitoringPointService")
class MonitoringPointServiceImpl
	extends AbstractMonitoringPointService
	with AutowiringMonitoringPointDaoComponent