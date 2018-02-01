package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}
import javax.sql.DataSource

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.JMap
import uk.ac.warwick.tabula.services.scheduling.HallOfResidenceImporter.HallOfResidenceInfo

import scala.collection.JavaConverters._
import scala.collection.JavaConversions.mapAsJavaMap

trait HallOfResidenceImporter {
	def getResidenceInfo(universityId: String): Option[HallOfResidenceInfo]
}

@Profile(Array("dev", "test", "production"))
@Service
class HallOfResidenceImporterImpl extends HallOfResidenceImporter {

	import HallOfResidenceImporter._

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	lazy val hallOfResidenceMappingQuery = new HallOfResidenceMappingQuery(sits)

	def getResidenceInfo(universityId: String): Option[HallOfResidenceInfo] = {
		hallOfResidenceMappingQuery.executeByNamedParam(Map("universityId" -> universityId)).asScala.toList match {
			case addressList: List[HallOfResidenceInfo] if addressList.nonEmpty =>  Some(addressList.head)
			case _ => None
		}
	}
}

@Profile(Array("sandbox"))
@Service
class SandboxHallOfResidenceImporter extends HallOfResidenceImporter {
	def getResidenceInfo(universityId: String): Option[HallOfResidenceInfo] =  Some(HallOfResidenceInfo("CH2/010", "Cryfield", "University of Warwick", "Coventry", "", "CV4 7ES", ""))
}

object HallOfResidenceImporter {
	var sitsSchema: String = Wire.property("${schema.sits}")

	var dialectRegexpLike = "regexp_like"

	// expecting hall address of format like XX1/202 where XX denotes building, 1 denotes block and remaining after / as room no OR TO21B (Tocil Room B, flat 21)
	def GetHallOfResidenceSql =
		f"""
			select add_add1, add_add2, add_add3, add_add4, add_add5, add_pcod, add_teln from $sitsSchema.men_add where add_aent = 'STU' and add_adid = :universityId and add_atyc = 'CORR' and add_actv = 'C' and add_dets = 'C'
			 and add_add3  =  'University of Warwick' and add_pcod in ('CV4 7AL', 'CV4 7ES') and $dialectRegexpLike (add_add1, '^[A-z]{2}[0-9]?\\/?[A-z0-9]+$$') and (add_begd is null or add_begd <= sysdate) and  (add_endd is null or add_endd >= sysdate) order by add_seqn desc
		"""

	case class HallOfResidenceInfo(var line1: String, var line2: String, var line3: String, var line4: String, var line5: String, var postcode: String, var telephone: String) {
		def isEmpty: Boolean = {
			!(StringUtils.hasText(line1) || StringUtils.hasText(line2) || StringUtils.hasText(line3) ||
				StringUtils.hasText(line4) || StringUtils.hasText(line5) || StringUtils.hasText(postcode) ||
				StringUtils.hasText(telephone))
		}
	}


	class HallOfResidenceMappingQuery(ds: DataSource)
		extends MappingSqlQueryWithParameters[(HallOfResidenceInfo)](ds, GetHallOfResidenceSql) {
		this.declareParameter(new SqlParameter("universityId", Types.VARCHAR))
		this.compile()

		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]): HallOfResidenceInfo = {
			HallOfResidenceInfo(
				line1 = rs.getString("add_add1"),
				line2 = rs.getString("add_add2"),
				line3 = rs.getString("add_add3"),
				line4 = rs.getString("add_add4"),
				line5 = rs.getString("add_add5"),
				postcode = rs.getString("add_pcod"),
				telephone = rs.getString("add_teln")
			)
		}
	}
}

trait HallOfResidenceImporterComponent {
	def hallOfResidenceImporter: HallOfResidenceImporter
}

trait AutowiringHallOfResidenceImporterComponent extends HallOfResidenceImporterComponent {
	var hallOfResidenceImporter: HallOfResidenceImporter = Wire[HallOfResidenceImporter]
}
