package uk.ac.warwick.tabula.scheduling.services

import java.sql.{ResultSet, Types}
import scala.collection.JavaConversions._
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import org.joda.time.DateTime

trait CasUsageImporter {
	// cas is a confirmation of acceptance to study - this determines whether such was used to apply for a visa
	def isCasUsed(universityId: String): Boolean
}

@Profile(Array("dev", "test", "production")) @Service
class CasUsageImporterImpl extends CasUsageImporter {
	import CasUsageImporter._

	var sits = Wire[DataSource]("sitsDataSource")

	lazy val casUsedMappingQuery = new CasUsedMappingQuery(sits)

	def isCasUsed(universityId: String) = {
		val earliestEndDate = DateTime.now.minusMonths(4).toDate
		val rowCount = casUsedMappingQuery.executeByNamedParam(Map("universityId" -> universityId, "earliestEndDate" -> earliestEndDate)).head
		rowCount.intValue > 0
	}
}

@Profile(Array("sandbox")) @Service
class SandboxCasUsageImporter extends CasUsageImporter {
	def isCasUsed(universityId: String) = false
}

object CasUsageImporter {
	var sitsSchema: String = Wire.property("${schema.sits}")

		// check to see if CAS is current or expired less than 3 months ago
		// VCR table in SITS is Visa CAS Request
		// CAS is Confirmation of Acceptance for Studies
    def CasUsedSql = f"""
 			select count(*) as count from $sitsSchema.srs_vcr
 			where vcr_stuc = :universityId
 			and vcr_iuse = 'Y'
 			and vcr_ukst in ('USED','ASSIGNED') -- CAS status
			and vcr_endd > :earliestEndDate
 		"""

	class CasUsedMappingQuery(ds: DataSource)
		extends MappingSqlQueryWithParameters[(Number)](ds, CasUsedSql) {
		this.declareParameter(new SqlParameter("universityId", Types.VARCHAR))
		this.declareParameter(new SqlParameter("earliestEndDate", Types.DATE))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]) = rs.getLong("count")
	}
}

trait CasUsageImporterComponent {
	def casUsageImporter: CasUsageImporter
}

trait AutowiringCasUsageImporterComponent extends CasUsageImporterComponent{
	var casUsageImporter = Wire[CasUsageImporter]
}

