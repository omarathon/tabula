package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import org.hibernate.criterion.Projections
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.TermService

trait AttendanceMonitoringDaoComponent {
	val attendanceMonitoringDao: AttendanceMonitoringDao
}

trait AutowiringAttendanceMonitoringDaoComponent extends AttendanceMonitoringDaoComponent {
	val attendanceMonitoringDao = Wire[AttendanceMonitoringDao]
}

trait AttendanceMonitoringDao {
	def listSchemes(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme]
	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String]
}


@Repository
class AttendanceMonitoringDaoImpl extends AttendanceMonitoringDao with Daoisms {

	def listSchemes(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme] = {
		session.newCriteria[AttendanceMonitoringScheme]
			.add(is("academicYear", academicYear))
			.add(is("department", department))
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

}