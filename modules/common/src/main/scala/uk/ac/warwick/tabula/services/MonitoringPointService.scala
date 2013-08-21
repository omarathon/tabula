package uk.ac.warwick.tabula.services

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{AutowiringMonitoringPointDaoComponent, MonitoringPointDaoComponent }
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint

trait MonitoringPointServiceComponent {
	def monitoringPointService: MonitoringPointService
}

trait AutowiringMonitoringPointServiceComponent extends MonitoringPointServiceComponent {
	var monitoringPointService = Wire[MonitoringPointService]
}

trait MonitoringPointService {
	def saveOrUpdate(monitoringPoint : MonitoringPoint)
	def getById(id : String) : Option[MonitoringPoint]
	def list(page: Int) : Seq[MonitoringPoint]
}


abstract class AbstractMonitoringPointService extends MonitoringPointService {
	self: MonitoringPointDaoComponent =>

	def saveOrUpdate(monitoringPoint: MonitoringPoint) = monitoringPointDao.saveOrUpdate(monitoringPoint)
	def getById(id: String): Option[MonitoringPoint] = monitoringPointDao.getById(id)
	def list(page: Int) : Seq[MonitoringPoint] = monitoringPointDao.list(page)
}

@Service("monitoringPointService")
class MonitoringPointServiceImpl
	extends AbstractMonitoringPointService
	with AutowiringMonitoringPointDaoComponent