package uk.ac.warwick.tabula.data

import scala.collection.JavaConverters._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpointState, MonitoringPointSet, MonitoringPointSetTemplate, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Route, StudentMember}
import org.hibernate.criterion.{Projections, Order}
import uk.ac.warwick.tabula.AcademicYear
import org.hibernate.criterion.Restrictions._

trait MonitoringPointDaoComponent {
	val monitoringPointDao: MonitoringPointDao
}

trait AutowiringMonitoringPointDaoComponent extends MonitoringPointDaoComponent {
	val monitoringPointDao = Wire[MonitoringPointDao]
}

trait MonitoringPointDao {
	def getPointById(id: String): Option[MonitoringPoint]
	def getSetById(id: String): Option[MonitoringPointSet]
	def getCheckpointsByStudent(monitoringPoints: Seq[MonitoringPoint]): Seq[(StudentMember, MonitoringCheckpoint)]
	def saveOrUpdate(monitoringPoint: MonitoringPoint)
	def delete(monitoringPoint: MonitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint)
	def saveOrUpdate(template: MonitoringPointSetTemplate)
	def saveOrUpdate(set: MonitoringPointSet)
	def findMonitoringPointSets(route: Route, academicYear: AcademicYear): Seq[MonitoringPointSet]
	def findMonitoringPointSets(route: Route): Seq[MonitoringPointSet]
	def findMonitoringPointSet(route: Route, academicYear: AcademicYear, year: Option[Int]): Option[MonitoringPointSet]
	def getCheckpoint(monitoringPoint: MonitoringPoint, student: StudentMember) : Option[MonitoringCheckpoint]
	def getCheckpoints(montitoringPoint: MonitoringPoint) : Seq[MonitoringCheckpoint]
	def listTemplates : Seq[MonitoringPointSetTemplate]
	def getTemplateById(id: String): Option[MonitoringPointSetTemplate]
	def deleteTemplate(template: MonitoringPointSetTemplate)
	def countCheckpointsForPoint(point: MonitoringPoint): Int
	def deleteCheckpoint(checkpoint: MonitoringCheckpoint): Unit
	def missedCheckpoints(student: StudentMember, academicYear: AcademicYear): Int
	def findPointSetsForStudents(students: Seq[StudentMember], academicYear: AcademicYear): Seq[MonitoringPointSet]
	def findPointSetsForStudentsByStudent(students: Seq[StudentMember], academicYear: AcademicYear): Map[StudentMember, MonitoringPointSet]
	def findSimilarPointsForMembers(point: MonitoringPoint, students: Seq[StudentMember]): Map[StudentMember, Seq[MonitoringPoint]]
	def studentsByMissedCount(
		universityIds: Seq[String],
		academicYear: AcademicYear,
		isAscending: Boolean,
		maxResults: Int,
		startResult: Int
	): Seq[StudentMember]
}


@Repository
class MonitoringPointDaoImpl extends MonitoringPointDao with Daoisms {


	def getPointById(id: String) =
		getById[MonitoringPoint](id)

	def getSetById(id: String) =
		getById[MonitoringPointSet](id)

	def getCheckpointsByStudent(monitoringPoints: Seq[MonitoringPoint]): Seq[(StudentMember, MonitoringCheckpoint)] = {
		val criteria = session.newCriteria[MonitoringCheckpoint]
			.createAlias("point", "point")

		val or = disjunction()
		monitoringPoints.grouped(Daoisms.MaxInClauseCount).foreach { mps => or.add(in("point", mps.asJava)) }
		criteria.add(or)

		val checkpoints = criteria.seq

		checkpoints.map(checkpoint => (checkpoint.studentCourseDetail.student, checkpoint))
	}

	def saveOrUpdate(monitoringPoint: MonitoringPoint) = session.saveOrUpdate(monitoringPoint)
	def delete(monitoringPoint: MonitoringPoint) = session.delete(monitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint) = session.saveOrUpdate(monitoringCheckpoint)
	def saveOrUpdate(set: MonitoringPointSet) = session.saveOrUpdate(set)
	def saveOrUpdate(template: MonitoringPointSetTemplate) = session.saveOrUpdate(template)

	def findMonitoringPointSets(route: Route) =
		session.newCriteria[MonitoringPointSet]
			.add(is("route", route))
			.seq

	def findMonitoringPointSets(route: Route, academicYear: AcademicYear) =
		session.newCriteria[MonitoringPointSet]
			.add(is("route", route))
			.add(is("academicYear", academicYear))
			.seq

	def findMonitoringPointSet(route: Route, academicYear: AcademicYear, year: Option[Int]) =
		session.newCriteria[MonitoringPointSet]
			.add(is("route", route))
			.add(is("academicYear", academicYear))
			.add(yearRestriction(year))
			.uniqueResult

	private def yearRestriction(opt: Option[Any]) = opt map { is("year", _) } getOrElse { isNull("year") }

	def getCheckpoint(monitoringPoint: MonitoringPoint, student: StudentMember): Option[MonitoringCheckpoint] = {
		student.mostSignificantCourseDetails match {
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
			.project[Number](Projections.rowCount())
			.uniqueResult.get.intValue()

	def deleteCheckpoint(checkpoint: MonitoringCheckpoint): Unit = {
		session.delete(checkpoint)
	}

	def missedCheckpoints(student: StudentMember, academicYear: AcademicYear): Int = {
		val scd = student.mostSignificantCourseDetails.getOrElse(throw new IllegalArgumentException)
		session.newCriteria[MonitoringCheckpoint]
			.add(is("studentCourseDetail", scd))
			.add(is("state", MonitoringCheckpointState.MissedUnauthorised))
			.createAlias("point", "point")
			.createAlias("point.pointSet", "pointSet")
			.add(is("pointSet.academicYear", academicYear))
			.project[Number](Projections.rowCount())
			.uniqueResult.get.intValue()
	}

	def findPointSetsForStudentsByStudent(students: Seq[StudentMember], academicYear: AcademicYear): Map[StudentMember, MonitoringPointSet] = {
		if (students.isEmpty)
			return Map()

		val partionedUniversityIdsWithIndex = students.map{_.universityId}.grouped(Daoisms.MaxInClauseCount).zipWithIndex.toSeq
		val queryString = """
			select distinct student, mps
			from MonitoringPointSet mps, Route r, StudentCourseDetails scd, StudentCourseYearDetails scyd, StudentMember student
			where r = mps.route
			and scd.route = r.code
			and scyd.studentCourseDetails = scd
			and student.mostSignificantCourse = scd
			and mps.academicYear = :academicYear
			and (
			  mps.year = scyd.yearOfStudy
			  or mps.year is null
			) and (
		""" +	partionedUniversityIdsWithIndex.map{
			case (ids, index) => "student.universityId in (:universityIds" + index.toString + ") "
		}.mkString(" or ")	+ ")"

		val query = session.newQuery[Array[java.lang.Object]](queryString)
			.setParameter("academicYear", academicYear)
		partionedUniversityIdsWithIndex.foreach{
			case (ids, index) => {
				query.setParameterList("universityIds" + index.toString, ids)
			}
		}

		query.seq.map{ objArray =>
			objArray(0).asInstanceOf[StudentMember] -> objArray(1).asInstanceOf[MonitoringPointSet]
		}.toMap
	}

	def findPointSetsForStudents(students: Seq[StudentMember], academicYear: AcademicYear): Seq[MonitoringPointSet] = {
		findPointSetsForStudentsByStudent(students, academicYear).values.toSeq
	}

	def findSimilarPointsForMembers(point: MonitoringPoint, students: Seq[StudentMember]): Map[StudentMember, Seq[MonitoringPoint]] = {
		if (students.isEmpty)
			return Map()

		val partionedUniversityIdsWithIndex = students.map{_.universityId}.grouped(Daoisms.MaxInClauseCount).zipWithIndex.toSeq

		val queryString = """
				select student, mp
				from StudentMember student, StudentCourseDetails scd, StudentCourseYearDetails scyd, Route r, MonitoringPointSet mps, MonitoringPoint mp
				where student.mostSignificantCourse = scd
				and scd.route = r.code
				and scd = scyd.studentCourseDetails
				and mps.route = r
				and mp.pointSet = mps
				and scyd.academicYear = mps.academicYear
				and (mps.year = scyd.yearOfStudy or mps.year is null)
				and mps.academicYear = :academicYear
				and lower(mp.name) = :name
				and mp.validFromWeek = :validFromWeek
				and mp.requiredFromWeek = :requiredFromWeek
				and (
		""" +	partionedUniversityIdsWithIndex.map{
			case (ids, index) => "student.universityId in (:universityIds" + index.toString + ") "
		}.mkString(" or ")	+ ")"

		val query = session.newQuery[Array[java.lang.Object]](queryString)
			.setParameter("academicYear", point.pointSet.asInstanceOf[MonitoringPointSet].academicYear)
			.setString("name", point.name.toLowerCase)
			.setString("validFromWeek", point.validFromWeek.toString)
			.setString("requiredFromWeek", point.requiredFromWeek.toString)
		partionedUniversityIdsWithIndex.foreach{
			case (ids, index) => {
				query.setParameterList("universityIds" + index.toString, ids)
			}
		}

		val memberPointMap = query.seq.map{objArray =>
			objArray(0).asInstanceOf[StudentMember] -> objArray(1).asInstanceOf[MonitoringPoint]
		}
		memberPointMap.groupBy(_._1).mapValues(_.map(_._2))
	}

	def studentsByMissedCount(
		universityIds: Seq[String],
		academicYear: AcademicYear,
		isAscending: Boolean,
		maxResults: Int,
		startResult: Int
	): Seq[StudentMember] = {
		if (universityIds.isEmpty)
			return Seq()

		val partionedUniversityIdsWithIndex = universityIds.grouped(Daoisms.MaxInClauseCount).zipWithIndex.toSeq

		val queryString = """
			select s.universityId, count(*) as missed from Member s, StudentCourseDetails scd, StudentCourseYearDetails scyd,
			Route r, MonitoringPointSet mps, MonitoringPoint mp, MonitoringCheckpoint mc
			where s.mostSignificantCourse = scd
			and scyd.studentCourseDetails = scd
			and scd.route = r.code
			and mps.route = r
			and (mps.year is null or mps.year = scyd.yearOfStudy)
			and mp.pointSet = mps
			and mc.point = mp
			and mc.studentCourseDetail = s.mostSignificantCourse
			and mps.academicYear = :academicYear
			and scyd.academicYear = :academicYear
			and mc.state = 'unauthorised'
			and (
		""" + partionedUniversityIdsWithIndex.map{
			case (ids, index) => "s.universityId in (:universityIds" + index.toString + ") "
		}.mkString(" or ") + """
			)
			group by s.universityId
		"""

		val query = session.newQuery[Array[java.lang.Object]](queryString)
			.setParameter("academicYear", academicYear)

		partionedUniversityIdsWithIndex.foreach{
			case (ids, index) => {
				query.setParameterList("universityIds" + index.toString, ids)
			}
		}

		val universityIdMissedMap = query.seq.map{objArray =>
			objArray(0).asInstanceOf[String] -> objArray(1).asInstanceOf[Long].toInt
		}.toMap
		val ordering = if (isAscending) Ordering[Int] else Ordering[Int].reverse

		val universityIdsToFetch = universityIds.map{u =>
			u -> universityIdMissedMap.getOrElse(u, 0)
		}.sortBy(_._2)(ordering).slice(startResult, startResult + maxResults + 1).map(_._1)

		val c = session.newCriteria[StudentMember]
		val or = disjunction()
		universityIdsToFetch.grouped(Daoisms.MaxInClauseCount).foreach { ids => or.add(in("universityId", ids.asJavaCollection)) }
		c.add(or)
		val students = c.seq

		universityIdsToFetch.flatMap{u => students.find(_.universityId == u)}.toSeq
	}
}