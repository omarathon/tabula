package uk.ac.warwick.tabula.data

import scala.collection.JavaConverters._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.spring.Wire

trait MonitoringPointDaoComponent {
	val monitoringPointDao: MonitoringPointDao
}

trait AutowiringMonitoringPointDaoComponent extends MonitoringPointDaoComponent {
	val monitoringPointDao = Wire[MonitoringPointDao]
}

trait MonitoringPointDao {
	def saveOrUpdate(monitoringPoint: MonitoringPoint)
	def getById(id: String): Option[MonitoringPoint]
	def list(page: Int) : Seq[MonitoringPoint]
}


@Repository
class MonitoringPointDaoImpl extends MonitoringPointDao with Daoisms {

	def saveOrUpdate(course: MonitoringPoint) = session.saveOrUpdate(course)

	def getById(id: String) = {
		val ret = session.newQuery[MonitoringPoint]("from MonitoringPoint monitoringPoint where id = :id").setString("id", id).uniqueResult
		ret
	}

	def list(page: Int) = {
		val perPage = 20
		val list = session.newCriteria[MonitoringPoint].setFirstResult(page).setMaxResults(perPage).list.asScala.toSeq
		list
	}

}
