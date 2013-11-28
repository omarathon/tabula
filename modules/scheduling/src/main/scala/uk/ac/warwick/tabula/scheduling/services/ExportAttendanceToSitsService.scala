package uk.ac.warwick.tabula.scheduling.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import javax.sql.DataSource
import org.springframework.jdbc.`object`.SqlUpdate
import java.sql.Types
import org.springframework.jdbc.core.SqlParameter
import uk.ac.warwick.tabula.scheduling.services.ExportAttendanceToSitsService.{ExportAttendanceToSitsCountQuery, ExportAttendanceToSitsUpdateQuery}
import uk.ac.warwick.tabula.JavaImports.JHashMap
import org.joda.time.DateTime
import org.springframework.jdbc.`object`.SqlQuery

trait ExportAttendanceToSitsServiceComponent {
	def exportAttendanceToSitsService: ExportAttendanceToSitsService
}

trait AutowiringExportAttendanceToSitsServiceComponent extends ExportAttendanceToSitsServiceComponent {
	var exportAttendanceToSitsService = Wire[ExportAttendanceToSitsService]
}

trait ExportAttendanceToSitsService {
	def exportToSits(report: MonitoringPointReport): Boolean
}

class AbstractExportAttendanceToSitsService extends ExportAttendanceToSitsService {

	self: SitsDataSourceComponent =>
	def exportToSits(report: MonitoringPointReport): Boolean = {
		val countQuery = new ExportAttendanceToSitsCountQuery(sitsDataSource)
		val count = countQuery.executeByNamedParam(JHashMap(("studentId", report.student.id)))

		val monitoringPeriod = {
			if (report.monitoringPeriod.indexOf("vacation") >= 0)
				s"${report.monitoringPeriod} ${report.academicYear.startYear}/${report.academicYear.endYear.toString.substring(2)}"
			else
				s"${report.monitoringPeriod} term ${report.academicYear.startYear}/${report.academicYear.endYear.toString.substring(2)}"
		}
		val parameterMap = JHashMap(
			("studentId", report.student.id),
			("counter", "%03d".format(count)),
			("now", DateTime.now),
			("academicYear", report.academicYear.toString),
			("deptCode", report.studentCourseYearDetails.enrolmentDepartment.code.toUpperCase),
			("courseCode", report.studentCourseDetails.course.code),
			("recorder", report.reporter),
			("missedPoints", report.missed),
			("monitoringPeriod", monitoringPeriod)
		)

		val updateQuery = new ExportAttendanceToSitsUpdateQuery(sitsDataSource)
		updateQuery.updateByNamedParam(parameterMap) == 1
	}
}

object ExportAttendanceToSitsService {
	final val GetExistingRowsSql = """
		 select sab_stuc from intuit.srs_sab
		 where sab_stuc = :studentId;
	"""

	final val PushToSITSSql = """
		insert into intuit.srs_sab
		(SAB_STUC,SAB_SEQ2,SAB_RAAC,SAB_ENDD,SAB_AYRC,SAB_UDF2,SAB_UDF3,SAB_UDF4,SAB_UDF5,SAB_UDFJ)
		values (:studentId, :counter,'UNAUTH', :now, :academicYear, :deptCode, :courseCode, :recorder, :missedPoints, :monitoringPeriod );
	"""

	class ExportAttendanceToSitsCountQuery(ds: DataSource) extends SqlQuery(ds, GetExistingRowsSql) {
		declareParameter(new SqlParameter("studentId", Types.VARCHAR))
	}

	class ExportAttendanceToSitsUpdateQuery(ds: DataSource) extends SqlUpdate(ds, PushToSITSSql) {

		declareParameter(new SqlParameter("studentId", Types.VARCHAR))
		declareParameter(new SqlParameter("counter", Types.VARCHAR))
		declareParameter(new SqlParameter("now", Types.DATE))
		declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
		declareParameter(new SqlParameter("deptCode", Types.VARCHAR))
		declareParameter(new SqlParameter("courseCode", Types.VARCHAR))
		declareParameter(new SqlParameter("recorder", Types.VARCHAR))
		declareParameter(new SqlParameter("missedPoints", Types.VARCHAR))
		declareParameter(new SqlParameter("monitoringPeriod", Types.VARCHAR))

	}
}



@Service
class ExportAttendanceToSitsServiceImpl
	extends AbstractExportAttendanceToSitsService with AutowiringSitsDataSourceComponent

trait SitsDataSourceComponent {
	def sitsDataSource: DataSource
}

trait AutowiringSitsDataSourceComponent extends SitsDataSourceComponent {
	var sitsDataSource = Wire[DataSource]("sitsDataSource")
}