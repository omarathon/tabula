package uk.ac.warwick.tabula.scheduling.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import javax.sql.DataSource
import org.springframework.jdbc.`object`.SqlUpdate
import java.sql.Types
import org.springframework.jdbc.core.SqlParameter
import uk.ac.warwick.tabula.scheduling.services.ExportAttendanceToSitsService.ExportAttendanceToSitsUpdateQuery
import java.util.HashMap

trait ExportAttendanceToSitsServiceComponent {
	def exportAttendanceToSitsService: ExportAttendanceToSitsService
}

trait AutowiringExportAttendanceToSitsServiceComponent extends ExportAttendanceToSitsServiceComponent {
	var exportAttendanceToSitsService = Wire[ExportAttendanceToSitsService]
}

trait ExportAttendanceToSitsService {
	def exportToSits(report: MonitoringPointReport): Boolean
}

class AbstractExportAttendanceToSitsService {

	self: SitsDataSourceComponent =>
	def exportToSits(report: MonitoringPointReport): Boolean = {
//		    sitsDataSource.

		val count = 0

		var parameterMap = new HashMap[String, Object]
		parameterMap.put("id", report.student.id);
		parameterMap.put("counter", count);
	// etc...


		val updateQuery = new ExportAttendanceToSitsUpdateQuery(sitsDataSource)
		updateQuery.
		false
	}
}

object ExportAttendanceToSitsService {
	final val pushToSITSSql = """
	insert into intuit.srs_sab
	(SAB_STUC,SAB_SEQ2,SAB_RAAC,SAB_ENDD,SAB_AYRC,SAB_UDF2,SAB_UDF3,SAB_UDF4,SAB_UDF5,SAB_UDFJ)
	values (:studentId, :counter,'UNAUTH', :now, :academicYear, :deptCode, :courseCode, :recorder, :missedPoints, :monitoringPeriod );"""

	class ExportAttendanceToSitsUpdateQuery(ds: DataSource) extends SqlUpdate(ds, pushToSITSSql) {

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