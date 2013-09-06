package uk.ac.warwick.tabula.scheduling.services

import java.sql.ResultSet
import java.sql.Types

import scala.collection.JavaConversions._
import scala.collection.immutable.HashMap

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service

import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.MemberDaoImpl
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.sandbox.MapResultSet
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportModuleRegistrationsCommand
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportStudentRowCommand
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.userlookup.User

/**
 * Import module registration data from SITS.
 *
 */

trait ModuleRegistrationImporter {
	def getModuleRegistrationDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportModuleRegistrationsCommand]
}

@Profile(Array("dev", "test", "production"))
@Service
class ModuleRegistrationImporterImpl extends ModuleRegistrationImporter with SitsAcademicYearAware {
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
class SandboxModuleRegistrationImporter extends ModuleRegistrationImporter {
	var memberDao = Wire.auto[MemberDaoImpl]

	def getModuleRegistrationDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportModuleRegistrationsCommand] =
		membersAndCategories flatMap { mac =>
			val usercode = mac.member.usercode
			val ssoUser = users(usercode)

			mac.member.userType match {
				case Student => studentModuleRegistrationDetails(usercode, ssoUser)
				case _ => Seq()
			}
		}

	def studentModuleRegistrationDetails(usercode: String, ssoUser: User) = {
		Seq()

/*
 *  Leaving this commented code in as it's how we might go about adding sandbox modreg data -
 *  but this implementation would register every single student onto AM101-30 which
 *  could be unwieldy if we implemented a page to list students by module, so it
 *  would need to be improved on to spread students over a suitable range of modules.
 *
    	val member = memberDao.getByUserId(usercode).get

		Seq(new ImportModuleRegistrationsCommand(
				new ModuleRegistrationRow(
					member.universityId + "/1",
					"AM101-30",
					30,
					"D",
					"C",
					AcademicYear(2013)
				)
			)
		)*/
	}
}

object ModuleRegistrationImporter {

	val GetModuleRegistration = """
			select scj_code, sms.mod_code, sms.sms_mcrd, sms.sms_agrp, sms.ses_code, sms.ayr_code
				from intuit.cam_sms sms, intuit.ins_stu stu, intuit.ins_spr spr, intuit.srs_scj scj, intuit.srs_vco, intuit.cam_ssn ssn
				where sms.spr_code = spr.spr_code
					and spr.spr_stuc = stu.stu_code
					and sms.ayr_code = :year
					and stu.stu_udf3 in (:usercodes)
					and scj.scj_sprc = spr.spr_code
					and vco_crsc = scj.scj_crsc
					and vco_rouc = spr.rou_code
					and sms.spr_code = ssn.ssn_sprc
					and ssn_ayrc = sms.ayr_code
					and ssn_mrgs != 'CON'
			union
			select scj_code, smo.mod_code, smo.smo_mcrd, smo.smo_agrp, smo.ses_code, smo.ayr_code
				from intuit.cam_smo smo, intuit.ins_stu stu, intuit.ins_spr spr, intuit.srs_scj scj, intuit.srs_vco, intuit.cam_ssn ssn
				where smo.spr_code = spr.spr_code
					and spr.spr_stuc = stu.stu_code
					and smo.ayr_code = :year
					and stu.stu_udf3 in (:usercodes)
					and scj.scj_sprc = spr.spr_code
					and vco_crsc = scj.scj_crsc
					and vco_rouc = spr.rou_code
					and ssn_mrgs = 'CON'
			union
			select scj_code, smo.mod_code, smo.smo_mcrd, smo.smo_agrp, smo.ses_code, smo.ayr_code
				from intuit.cam_smo smo, intuit.ins_stu stu, intuit.ins_spr spr, intuit.srs_scj scj, intuit.srs_vco
				where smo.spr_code = spr.spr_code
				and spr.spr_stuc = stu.stu_code
				and smo.ayr_code = :year
				and stu.stu_udf3 in (:usercodes)
				and scj.scj_sprc = spr.spr_code
				and vco_crsc = scj.scj_crsc
				and vco_rouc = spr.rou_code
				and not exists (select * from intuit.cam_ssn
					where ssn_sprc = smo.spr_code
					and ssn_ayrc = smo.ayr_code)
			"""

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
