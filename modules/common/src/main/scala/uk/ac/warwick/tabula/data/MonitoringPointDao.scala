package uk.ac.warwick.tabula.data

import scala.collection.JavaConverters._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{StudentMember, Member}

trait MonitoringPointDaoComponent {
	val monitoringPointDao: MonitoringPointDao
}

trait AutowiringMonitoringPointDaoComponent extends MonitoringPointDaoComponent {
	val monitoringPointDao = Wire[MonitoringPointDao]
}

trait MonitoringPointDao {
	def getById(id: String): Option[MonitoringPoint]
	def list(page: Int) : Seq[MonitoringPoint]
	def getStudentsChecked(monitoringPoint: MonitoringPoint): Seq[StudentMember]
	def saveOrUpdate(monitoringPoint: MonitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint)
	def getCheckpoint(monitoringPoint: MonitoringPoint, member: StudentMember) : Option[MonitoringCheckpoint]
	def getCheckpoints(montitoringPoint: MonitoringPoint) : Seq[MonitoringCheckpoint]
}


@Repository
class MonitoringPointDaoImpl extends MonitoringPointDao with Daoisms {


	def getById(id: String) = {
		val ret = session.newQuery[MonitoringPoint]("from MonitoringPoint monitoringPoint where id = :id").setString("id", id).uniqueResult
		ret
	}

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

	def getCheckpoint(monitoringPoint: MonitoringPoint, member: StudentMember): Option[MonitoringCheckpoint] = {
	  val studentCourseDetail = member.studentCourseDetails.asScala.filter(_.route == monitoringPoint.pointSet.route).head

		session.newCriteria[MonitoringCheckpoint]
			.add(is("studentCourseDetail", studentCourseDetail))
			.uniqueResult
	}

	def getCheckpoints(monitoringPoint: MonitoringPoint) : Seq[MonitoringCheckpoint] = {
		session.newCriteria[MonitoringCheckpoint]
			.add(is("point", monitoringPoint))
			.seq
	}


}
