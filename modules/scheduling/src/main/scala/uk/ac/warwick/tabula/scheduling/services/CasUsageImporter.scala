package uk.ac.warwick.tabula.scheduling.services;

import java.sql.{ResultSet, Types}

import scala.collection.JavaConversions._
import scala.math.BigDecimal.int2bigDecimal

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service

import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._

trait CasUsageImporter {
	/**
	 * Returns a sequence of pairs of PRS codes and the percentage load
	 */
	def isCasUsed(universityId: String): Boolean // cas is a confirmation of acceptance to study - this determines whether such was used to apply for a visa
}

@Profile(Array("dev", "test", "production")) @Service
class CasUsageImporterImpl extends CasUsageImporter with SitsAcademicYearAware{
	import CasUsageImporter._

	var sits = Wire[DataSource]("sitsDataSource")

	lazy val casUsedMappingQuery = new CasUsedMappingQuery(sits)

	def isCasUsed(universityId: String): Boolean = {
		val rowCount = casUsedMappingQuery.executeByNamedParam(Map("universityId" -> universityId, "year" -> getCurrentSitsAcademicYearString)).head
		(rowCount.intValue() > 0)
	}
}

@Profile(Array("sandbox")) @Service
class SandboxCasUsageImporter extends CasUsageImporter {
	def isCasUsed(universityId: String): Boolean = false
}

object CasUsageImporter {
	var sitsSchema: String = Wire.property("${schema.sits}")

    val CasUsedSql = f"""
 			select count(*) as count from $sitsSchema.srs_vcr
 			where vcr_stuc = :universityId
 			and vcr_ayrc = :year
 			and vcr_iuse = 'Y'
 			and vcr_ukst = 'USED'
    	"""


	class CasUsedMappingQuery(ds: DataSource)
		extends MappingSqlQueryWithParameters[(Number)](ds, CasUsedSql) {
		this.declareParameter(new SqlParameter("universityId", Types.VARCHAR))
		this.declareParameter(new SqlParameter("year", Types.VARCHAR))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _])= {
			(rs.getLong("count"))
		}
	}
}

trait CasUsageImporterComponent {
	def casUsageImporter: CasUsageImporter
}

trait AutowiringCasUsageImporterComponent extends CasUsageImporterComponent{
	var casUsageImporter = Wire[CasUsageImporter]
}

