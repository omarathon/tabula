package uk.ac.warwick.tabula.scheduling.services;

import org.springframework.stereotype.Service
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters
import java.sql.ResultSet
import org.springframework.jdbc.core.SqlParameter
import javax.sql.DataSource
import java.sql.Types
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.spring.Wire
import scala.collection.JavaConverters._
import collection.JavaConversions._

@Service
class SupervisorImporter  {
	import SupervisorImporter._

	var sits = Wire[DataSource]("sitsDataSource")

	lazy val supervisorMappingQuery = new SupervisorMappingQuery(sits)

	def getSupervisorPrsCodes(scjCode: String): Seq[String] = {
		val supervisorPrsCodes = supervisorMappingQuery.executeByNamedParam(Map("scj_code" -> scjCode))
		//supervisorPrsCodes.asScala

		supervisorMappingQuery.executeByNamedParam(Map("scj_code" -> scjCode))
	}
}

object SupervisorImporter {

	val GetSupervisorsSql = "select rdx_prsc from intuit.srs_rdx where rdx_scjc = ?"

	class SupervisorMappingQuery(ds: DataSource) extends MappingSqlQueryWithParameters[String](ds, GetSupervisorsSql) {
		this.declareParameter(new SqlParameter("scj_code", Types.VARCHAR))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]) = {
			rs.getString("rdx_prsc")
		}
	}
}
