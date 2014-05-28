package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import org.hibernate.criterion.Projections
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.TermService
import org.hibernate.criterion.Restrictions._

abstract class SchemeMembershipItemType(val value: String)
case object SchemeMembershipStaticType extends SchemeMembershipItemType("static")
case object SchemeMembershipIncludeType extends SchemeMembershipItemType("include")
case object SchemeMembershipExcludeType extends SchemeMembershipItemType("exclude")

/**
 * Item in list of members for displaying in view.
 */
case class SchemeMembershipItem(
	itemType: SchemeMembershipItemType, // static, include or exclude
	firstName: String,
	lastName: String,
	universityId: String,
	userId: String,
	existingSchemes: Seq[AttendanceMonitoringScheme]
) {
	def itemTypeString = itemType.value
}

trait AttendanceMonitoringDaoComponent {
	val attendanceMonitoringDao: AttendanceMonitoringDao
}

trait AutowiringAttendanceMonitoringDaoComponent extends AttendanceMonitoringDaoComponent {
	val attendanceMonitoringDao = Wire[AttendanceMonitoringDao]
}

trait AttendanceMonitoringDao {
	def getSchemeById(id: String): Option[AttendanceMonitoringScheme]
	def getPointById(id: String): Option[AttendanceMonitoringPoint]
	def saveOrUpdate(scheme: AttendanceMonitoringScheme): Unit
	def saveOrUpdate(point: AttendanceMonitoringPoint): Unit
	def delete(scheme: AttendanceMonitoringScheme)
	def listSchemes(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme]
	def listOldSets(department: Department, academicYear: AcademicYear): Seq[MonitoringPointSet]
	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String]
	def findReports(studentIds: Seq[String], year: AcademicYear, period: String): Seq[MonitoringPointReport]
	def findSchemeMembershipItems(universityIds: Seq[String], itemType: SchemeMembershipItemType): Seq[SchemeMembershipItem]
	def findPoints(
		department: Department,
		academicYear: AcademicYear,
		schemes: Seq[AttendanceMonitoringScheme],
		types: Seq[AttendanceMonitoringPointType],
		styles: Seq[AttendanceMonitoringPointStyle]
	): Seq[AttendanceMonitoringPoint]
	def findOldPoints(
		department: Department,
		academicYear: AcademicYear,
		sets: Seq[MonitoringPointSet],
		types: Seq[MonitoringPointType]
	): Seq[MonitoringPoint]
	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]
	def getAttendanceNote(student: StudentMember, point: AttendanceMonitoringPoint): Option[AttendanceMonitoringNote]
	def countCheckpointsForPoint(point: AttendanceMonitoringPoint): Int
}


@Repository
class AttendanceMonitoringDaoImpl extends AttendanceMonitoringDao with Daoisms {

	def getSchemeById(id: String): Option[AttendanceMonitoringScheme] =
		getById[AttendanceMonitoringScheme](id)

	def getPointById(id: String): Option[AttendanceMonitoringPoint] =
		getById[AttendanceMonitoringPoint](id)

	def saveOrUpdate(scheme: AttendanceMonitoringScheme): Unit =
		session.saveOrUpdate(scheme)

	def saveOrUpdate(point: AttendanceMonitoringPoint): Unit =
		session.saveOrUpdate(point)

	def delete(scheme: AttendanceMonitoringScheme) =
		session.delete(scheme)

	def listSchemes(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme] = {
		session.newCriteria[AttendanceMonitoringScheme]
			.add(is("academicYear", academicYear))
			.add(is("department", department))
			.seq
	}

	def listOldSets(department: Department, academicYear: AcademicYear): Seq[MonitoringPointSet] = {
		session.newCriteria[MonitoringPointSet]
			.createAlias("route", "route")
			.add(is("academicYear", academicYear))
			.add(is("route.department", department))
			.seq
	}

	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String] = {
		if (students.isEmpty)
			return Seq()

		val termCounts =
			session.newCriteria[MonitoringPointReport]
				.add(is("academicYear", academicYear))
				.add(safeIn("student.universityId", students.map(_.universityId)))
				.project[Array[java.lang.Object]](
					Projections.projectionList()
						.add(Projections.groupProperty("monitoringPeriod"))
						.add(Projections.count("monitoringPeriod"))
				)
				.seq
				.map { objArray =>
				objArray(0).asInstanceOf[String] -> objArray(1).asInstanceOf[Long].toInt
			}

		TermService.orderedTermNames.diff(termCounts.filter{case(term, count) => count.intValue() == students.size}.map { _._1})
	}

	def findReports(studentsIds: Seq[String], academicYear: AcademicYear, period: String): Seq[MonitoringPointReport] = {
		if (studentsIds.isEmpty)
			return Seq()

		session.newCriteria[MonitoringPointReport]
			.add(is("academicYear", academicYear))
			.add(is("monitoringPeriod", period))
			.add(safeIn("student.universityId", studentsIds))
			.seq
	}

	def findSchemeMembershipItems(universityIds: Seq[String], itemType: SchemeMembershipItemType): Seq[SchemeMembershipItem] = {
		if (universityIds.isEmpty)
			return Seq()

		val items = session.newCriteria[StudentMember]
			.add(safeIn("universityId", universityIds))
			.project[Array[java.lang.Object]](
				Projections.projectionList()
					.add(Projections.property("firstName"))
					.add(Projections.property("lastName"))
					.add(Projections.property("universityId"))
					.add(Projections.property("userId"))
			).seq.map { objArray =>
				SchemeMembershipItem(
					itemType,
					objArray(0).asInstanceOf[String],
					objArray(1).asInstanceOf[String],
					objArray(2).asInstanceOf[String],
					objArray(3).asInstanceOf[String],
					Seq() // mixed in by the service
				)
			}

		// keep the same order
		universityIds.map(uniId => items.find(_.universityId == uniId)).flatten
	}

	def findPoints(
		department: Department,
		academicYear: AcademicYear,
		schemes: Seq[AttendanceMonitoringScheme],
		types: Seq[AttendanceMonitoringPointType],
		styles: Seq[AttendanceMonitoringPointStyle]
	): Seq[AttendanceMonitoringPoint] = {
		val query = session.newCriteria[AttendanceMonitoringPoint]
			.createAlias("scheme", "scheme")
			.add(is("scheme.department", department))
			.add(is("scheme.academicYear", academicYear))

		if (!schemes.isEmpty)
			query.add(safeIn("scheme", schemes))
		if (!types.isEmpty)
			query.add(safeIn("pointType", types))
		if (!styles.isEmpty)
			query.add(safeIn("scheme.pointStyle", styles))

		query.seq
	}

	def findOldPoints(
		department: Department,
		academicYear: AcademicYear,
		sets: Seq[MonitoringPointSet],
		types: Seq[MonitoringPointType]
	): Seq[MonitoringPoint] = {
		val query = session.newCriteria[MonitoringPoint]
			.createAlias("pointSet", "pointSet")
			.createAlias("pointSet.route", "route")
			.add(is("route.department", department))
			.add(is("pointSet.academicYear", academicYear))

		if(!sets.isEmpty)
			query.add(safeIn("pointSet", sets))
		if (!types.isEmpty) {
			if (types.contains(null)) {
				query.add(disjunction().add(safeIn("pointType", types)).add(isNull("pointType")))
			} else {
				query.add(safeIn("pointType", types))
			}
		}

		query.seq
	}

	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint] = {
		val checkpoints = session.newCriteria[AttendanceMonitoringCheckpoint]
			.add(is("student", student))
			.add(safeIn("point", points))
			.seq

		checkpoints.map{ c => c.point -> c}.toMap
	}

	def getAttendanceNote(student: StudentMember, point: AttendanceMonitoringPoint): Option[AttendanceMonitoringNote] = {
		session.newCriteria[AttendanceMonitoringNote]
			.add(is("student", student))
			.add(is("point", point))
			.uniqueResult
	}

	def countCheckpointsForPoint(point: AttendanceMonitoringPoint) =
		session.newCriteria[AttendanceMonitoringCheckpoint]
			.add(is("point", point))
			.project[Number](Projections.rowCount())
			.uniqueResult.get.intValue()

}