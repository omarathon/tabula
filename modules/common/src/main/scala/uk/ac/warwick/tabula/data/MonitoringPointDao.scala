package uk.ac.warwick.tabula.data

import scala.collection.JavaConverters._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPointSetTemplate, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.StudentMember
import org.hibernate.criterion.Order

trait MonitoringPointDaoComponent {
	val monitoringPointDao: MonitoringPointDao
}

trait AutowiringMonitoringPointDaoComponent extends MonitoringPointDaoComponent {
	val monitoringPointDao = Wire[MonitoringPointDao]
}

trait MonitoringPointDao {
	def getPointById(id: String): Option[MonitoringPoint]
	def getSetById(id: String): Option[MonitoringPointSet]
	def list(page: Int) : Seq[MonitoringPoint]
	def getStudentsChecked(monitoringPoint: MonitoringPoint): Seq[StudentMember]
	def saveOrUpdate(monitoringPoint: MonitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint)
	def saveOrUpdate(set: MonitoringPointSetTemplate)
	def getCheckpoint(monitoringPoint: MonitoringPoint, member: StudentMember) : Option[MonitoringCheckpoint]
	def getCheckpoints(montitoringPoint: MonitoringPoint) : Seq[MonitoringCheckpoint]
	def listTemplates : Seq[MonitoringPointSetTemplate]
	def getTemplateById(id: String): Option[MonitoringPointSetTemplate]
	def deleteTemplate(template: MonitoringPointSetTemplate)
}


@Repository
class MonitoringPointDaoImpl extends MonitoringPointDao with Daoisms {


	def getPointById(id: String) =
		getById[MonitoringPoint](id)

	def getSetById(id: String) =
		getById[MonitoringPointSet](id)

	def list(page: Int) = {
		val perPage = 20
		session.newCriteria[MonitoringPoint]
			.setFirstResult(page)
			.setMaxResults(perPage)
			.list.asScala.toSeq
	}

	def getStudentsChecked(monitoringPoint: MonitoringPoint): Seq[StudentMember] = {
		val checkpoints = session.newQuery[MonitoringCheckpoint]("from MonitoringCheckpoint where point = :point_id and checked = true")
				.setString("point_id", monitoringPoint.id)
				.seq

		checkpoints.map(_.studentCourseDetail.student)
	}

	def saveOrUpdate(monitoringPoint: MonitoringPoint) = session.saveOrUpdate(monitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint) = session.saveOrUpdate(monitoringCheckpoint)
	def saveOrUpdate(set: MonitoringPointSetTemplate) = session.saveOrUpdate(set)

	def getCheckpoint(monitoringPoint: MonitoringPoint, member: StudentMember): Option[MonitoringCheckpoint] = {
	  val studentCourseDetail = member.studentCourseDetails.asScala.filter(
			_.route == monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet].route
		).head

		session.newCriteria[MonitoringCheckpoint]
			.add(is("studentCourseDetail", studentCourseDetail))
			.uniqueResult
	}

	def getCheckpoints(monitoringPoint: MonitoringPoint) : Seq[MonitoringCheckpoint] = {
		session.newCriteria[MonitoringCheckpoint]
			.add(is("point", monitoringPoint))
			.seq
	}

	def listTemplates: Seq[MonitoringPointSetTemplate] = {
		session.newCriteria[MonitoringPointSetTemplate]
			.addOrder(Order.asc("position"))
			.list.asScala.toSeq
	}

	def getTemplateById(id: String): Option[MonitoringPointSetTemplate] =
		getById[MonitoringPointSetTemplate](id)

	def deleteTemplate(template: MonitoringPointSetTemplate) = session.delete(template)
}