package uk.ac.warwick.tabula.scheduling.services

import java.sql.ResultSet
import scala.collection.JavaConversions._
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service
import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.helpers.Logging
import org.apache.log4j.Logger
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportModuleRegistrationsCommand
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.ItemNotFoundException
import org.springframework.jdbc.core.SqlParameter
import uk.ac.warwick.tabula.AcademicYear
import java.sql.Types
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import scala.collection.immutable.HashMap

/**
 * Import module registration data from SITS.
 *
 */

@Profile(Array("dev", "test", "production"))
@Service
class ModuleRegistrationImporter extends SitsAcademicYearAware {
	import ModuleRegistrationImporter._

	def moduleRegistrationQuery() = {
		new ModuleRegistrationQuery(sits)
	}

	def getModuleRegistrationDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportModuleRegistrationsCommand] = {
		val sitsCurrentAcademicYear = getCurrentSitsAcademicYearString

		membersAndCategories flatMap { mac =>
			val usercode = mac.member.usercode
			val ssoUser = users(usercode)

			mac.member.userType match {
				case Student => {
					var params = HashMap(("year", sitsCurrentAcademicYear), ("usercodes", usercode))
					moduleRegistrationQuery().executeByNamedParam(params).toSeq

					}
				case _ => Seq()
			}
		}
	}
}

@Profile(Array("sandbox")) @Service
class SandboxModuleRegistrationImporter {
}

object ModuleRegistrationImporter {

	val GetModuleRegistration = """
		select scj_code, sms.mod_code, sms.sms_mcrd, sms.sms_agrp, sms.ses_code, sms.ayr_code
			from intuit.cam_sms sms, intuit.ins_stu stu, intuit.ins_spr spr, intuit.srs_scj scj, intuit.srs_vco
			where sms.spr_code = spr.spr_code
			and spr.spr_stuc = stu.stu_code
			and sms.ayr_code = :year
			and stu.stu_udf3 in (:usercodes)
			and scj.scj_sprc = spr.spr_code
			and vco_crsc = scj.scj_crsc
			and vco_rouc = spr.rou_code
		"""

/*	val GetModuleRegistration = """
		select sms.spr_code, sms.mod_code, sms.sms_mcrd, sms.sms_agrp, sms.ses_code, sms.ayr_code
		from intuit.cam_sms sms, intuit.ins_stu stu, intuit.ins_spr spr
		where sms.spr_code = spr.spr_code
		and spr.spr_stuc = stu.stu_code
		and sms.ayr_code = :year and stu.stu_udf3 in (:usercodes)
		"""
*/
	class ModuleRegistrationQuery(ds: DataSource)
		extends MappingSqlQuery[ImportModuleRegistrationsCommand](ds, GetModuleRegistration) {
			declareParameter(new SqlParameter("usercodes", Types.VARCHAR))
			declareParameter(new SqlParameter("year", Types.VARCHAR))
			compile()
			override def mapRow(resultSet: ResultSet, rowNumber: Int) = {
				val modRegRow = new ModuleRegistrationRow(
						resultSet.getString("scj_code"),
						resultSet.getString("mod_code"),
						resultSet.getDouble("sms_mcrd"),
						resultSet.getString("sms_agrp"),
						resultSet.getString("ses_code"),
						AcademicYear.parse(resultSet.getString("ayr_code")) )// shouldn't need to parse this out of the result set, must be a better way ...

				new ImportModuleRegistrationsCommand(modRegRow)
			}
	}
}

class ModuleRegistrationRow(
	val scjCode: String,
	val sitsModuleCode: String,
	val cats: Double,
	val assessmentGroup: String,
	val selectionStatusCode: String,
	val academicYear: AcademicYear) {

	var madService = Wire.auto[ModuleAndDepartmentService]

	def tabulaModule = madService.getModuleBySitsCode(sitsModuleCode)
}
