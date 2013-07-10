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
	def getSupervisorPrsCodes(scjCode: String): Seq[String]
}

@Profile(Array("dev", "test", "production")) @Service
class SupervisorImporterImpl extends SupervisorImporter {
	import SupervisorImporter._

	var sits = Wire[DataSource]("sitsDataSource")

	lazy val supervisorMappingQuery = new SupervisorMappingQuery(sits)

	def getSupervisorPrsCodes(scjCode: String): Seq[String] = {
		val supervisorPrsCodes = supervisorMappingQuery.executeByNamedParam(Map("scj_code" -> scjCode))

		supervisorMappingQuery.executeByNamedParam(Map("scj_code" -> scjCode))
	}
}

@Profile(Array("sandbox")) @Service
class SandboxSupervisorImporter extends SupervisorImporter {
	def getSupervisorPrsCodes(scjCode: String): Seq[String] = Seq.empty // TODO
}

object SupervisorImporter {

	val GetSupervisorsSql = "select rdx_prsc from intuit.srs_rdx where rdx_scjc = :scj_code and rdx_extc = 'SUP'"

	class SupervisorMappingQuery(ds: DataSource) extends MappingSqlQueryWithParameters[String](ds, GetSupervisorsSql) {
		this.declareParameter(new SqlParameter("scj_code", Types.VARCHAR))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]) = {
			rs.getString("rdx_prsc")
		}
	}
}
