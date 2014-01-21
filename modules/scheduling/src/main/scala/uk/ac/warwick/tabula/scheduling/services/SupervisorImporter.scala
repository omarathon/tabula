package uk.ac.warwick.tabula.scheduling.services;

import java.sql.ResultSet
import java.sql.Types
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.context.annotation.Profile

trait SupervisorImporter {
	/**
	 * Returns a sequence of pairs of PRS codes and the percentage load
	 */

	val ResearchSupervisorSitsExaminerType = "SUP"
	val DissertationSupervisorSitsExaminerType = "WASUP"

	val UrlPartToSitsExaminerTypeMap = Map(
			"supervisor" -> ResearchSupervisorSitsExaminerType,
			"dissertation-supervisor" -> DissertationSupervisorSitsExaminerType
	);

	def getSupervisorUniversityIds(scjCode: String, sitsExaminerType: String): Seq[(String, JBigDecimal)]
}

@Profile(Array("dev", "test", "production")) @Service
class SupervisorImporterImpl extends SupervisorImporter {
	import SupervisorImporter._

	var sits = Wire[DataSource]("sitsDataSource")

	lazy val supervisorMappingQuery = new SupervisorMappingQuery(sits)

	def getSupervisorUniversityIds(scjCode: String, urlPart: String): Seq[(String, JBigDecimal)] = {
		supervisorMappingQuery.executeByNamedParam(Map("scj_code" -> scjCode, "sits_examiner_type" -> UrlPartToSitsExaminerTypeMap(urlPart)))
	}
}

@Profile(Array("sandbox")) @Service
class SandboxSupervisorImporter extends SupervisorImporter {
	def getSupervisorUniversityIds(scjCode: String, sitsExaminerType: String): Seq[(String, JBigDecimal)] = Seq.empty // TODO
}

object SupervisorImporter {
	var sitsSchema: String = Wire.property("${schema.sits}")

	val GetSupervisorsSql = f"""
		select
			prs_udf1,
			rdx_perc
		from $sitsSchema.srs_rdx rdx, $sitsSchema.ins_prs prs
		where rdx_scjc = :scj_code
		and rdx_extc = :sits_examiner_type
		and rdx.rdx_prsc = prs.prs_code
		"""

	class SupervisorMappingQuery(ds: DataSource) extends MappingSqlQueryWithParameters[(String, JBigDecimal)](ds, GetSupervisorsSql) {
		this.declareParameter(new SqlParameter("scj_code", Types.VARCHAR))
		this.declareParameter(new SqlParameter("sits_examiner_type", Types.VARCHAR))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]) = {
			(rs.getString("prs_udf1"), rs.getBigDecimal("rdx_perc"))
		}
	}
}
