package uk.ac.warwick.tabula.data

import scala.collection.JavaConverters._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPointSetTemplate, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.StudentMember
import org.hibernate.criterion.{Projections, Order}

trait MonitoringPointDaoComponent {
	val monitoringPointDao: MonitoringPointDao
}

trait AutowiringMonitoringPointDaoComponent extends MonitoringPointDaoComponent {
	val monitoringPointDao = Wire[MonitoringPointDao]
}

trait MonitoringPointDao {
	def getPointById(id: String): Option[MonitoringPoint]
	def getSetById(id: String): Option[MonitoringPointSet]
	def getStudents(monitoringPoint: MonitoringPoint): Seq[(StudentMember, Boolean)]
	def saveOrUpdate(monitoringPoint: MonitoringPoint)
	def delete(monitoringPoint: MonitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint)
	def saveOrUpdate(template: MonitoringPointSetTemplate)
	def saveOrUpdate(set: MonitoringPointSet)
	def getCheckpoint(monitoringPoint: MonitoringPoint, member: StudentMember) : Option[MonitoringCheckpoint]
	def getCheckpoints(montitoringPoint: MonitoringPoint) : Seq[MonitoringCheckpoint]
	def listTemplates : Seq[MonitoringPointSetTemplate]
	def getTemplateById(id: String): Option[MonitoringPointSetTemplate]
	def deleteTemplate(template: MonitoringPointSetTemplate)
	def countCheckpointsForPoint(point: MonitoringPoint): Int
}


@Repository
class MonitoringPointDaoImpl extends MonitoringPointDao with Daoisms {


	def getPointById(id: String) =
		getById[MonitoringPoint](id)

	def getSetById(id: String) =
		getById[MonitoringPointSet](id)

	def getStudents(monitoringPoint: MonitoringPoint): Seq[(StudentMember, Boolean)] = {
		val checkpoints = session.newQuery[MonitoringCheckpoint]("from MonitoringCheckpoint where point = :point_id")
				.setString("point_id", monitoringPoint.id)
				.seq

		checkpoints.map(checkpoint => (checkpoint.studentCourseDetail.student, checkpoint.checked))
	}

	def saveOrUpdate(monitoringPoint: MonitoringPoint) = session.saveOrUpdate(monitoringPoint)
	def delete(monitoringPoint: MonitoringPoint) = session.delete(monitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint) = session.saveOrUpdate(monitoringCheckpoint)
	def saveOrUpdate(set: MonitoringPointSet) = session.saveOrUpdate(set)
	def saveOrUpdate(template: MonitoringPointSetTemplate) = session.saveOrUpdate(template)

	def getCheckpoint(monitoringPoint: MonitoringPoint, member: StudentMember): Option[MonitoringCheckpoint] = {
	  val studentCourseDetailOption = member.studentCourseDetails.asScala.find(
			_.route == monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet].route
		)

		studentCourseDetailOption match {
			case Some(studentCourseDetail) => session.newCriteria[MonitoringCheckpoint]
				.add(is("studentCourseDetail", studentCourseDetail))
				.add(is("point", monitoringPoint))
				.uniqueResult
			case _ => None
		}
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

	def countCheckpointsForPoint(point: MonitoringPoint) =
		session.newCriteria[MonitoringCheckpoint]
			.add(is("point", point))
			.setProjection(Projections.rowCount())
			.uniqueResult.get.asInstanceOf[Number].intValue()
}