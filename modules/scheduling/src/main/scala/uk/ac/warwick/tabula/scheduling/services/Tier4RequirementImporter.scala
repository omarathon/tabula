package uk.ac.warwick.tabula.scheduling.services;

import java.sql.{ResultSet, Types}

import scala.collection.JavaConversions.{asScalaBuffer, mapAsJavaMap}
import scala.math.BigDecimal.{int2bigDecimal, javaBigDecimal2bigDecimal}

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service

import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.JMap

trait Tier4RequirementImporter {
	/**
	 * Returns a sequence of pairs of PRS codes and the percentage load
	 */
	def hasTier4Requirement(universityId: String): Boolean
}

@Profile(Array("dev", "test", "production")) @Service
class Tier4RequirementImporterImpl extends Tier4RequirementImporter {
	import Tier4RequirementImporter._

	var sits = Wire[DataSource]("sitsDataSource")

	lazy val tier4RequirementMappingQuery = new Tier4RequirementMappingQuery(sits)

	def hasTier4Requirement(universityId: String): Boolean = {
		val numNationalitiesNotNeedingVisa =
			tier4RequirementMappingQuery.executeByNamedParam(Map("universityId" -> universityId)).head
		if (numNationalitiesNotNeedingVisa > 0) false else true
	}
}

@Profile(Array("sandbox")) @Service
class SandboxTier4RequirementImporter extends Tier4RequirementImporter {
	def hasTier4Requirement(universityId: String): Boolean = false
}

object Tier4RequirementImporter {
	var sitsSchema: String = Wire.property("${schema.sits}")

	val GetTier4RequirementSql = f"""
			select count(nat_edid) as count from $sitsSchema.srs_nat
			where nat_code in (
				(select stu_natc from $sitsSchema.ins_stu where stu_code = :universityId),
				(select stu_nat1 from $sitsSchema.ins_stu where stu_code = :universityId))
			and nat_edid = 0
			and nat_iuse = 'Y'
		"""

	class Tier4RequirementMappingQuery(ds: DataSource)
		extends MappingSqlQueryWithParameters[(BigDecimal)](ds, GetTier4RequirementSql) {
		this.declareParameter(new SqlParameter("universityId", Types.VARCHAR))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]) = {
			(rs.getBigDecimal("count"))
		}
	}

}
