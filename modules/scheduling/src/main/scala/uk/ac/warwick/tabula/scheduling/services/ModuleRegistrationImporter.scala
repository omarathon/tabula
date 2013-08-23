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
import uk.ac.warwick.tabula.data.ModuleRegistrationDao
import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportModuleRegistrationsCommand
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.ItemNotFoundException

/**
 * Import module registration data from SITS.
 *
 */

@Profile(Array("dev", "test", "production"))
@Service
class ModuleRegistrationImporter extends SitsAcademicYearAware {
	import ModuleRegistrationImporter._

	def moduleRegistrationQuery(member: MembershipInformation, ssoUser: User) = {
		new ModuleRegistrationQuery(sits)
	}

	def getModuleRegistrationDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportModuleRegistrationsCommand] = {
		val sitsCurrentAcademicYear = getCurrentSitsAcademicYearString

		membersAndCategories flatMap { mac =>
			val usercode = mac.member.usercode
			val ssoUser = users(usercode)

			mac.member.userType match {
				case Student => {
					moduleRegistrationQuery(mac, ssoUser).executeByNamedParam(
											Map("year" -> sitsCurrentAcademicYear, "usercodes" -> usercode)
										  ).toSeq
					}
				case _ => Seq()
			}
		}
	}

}

class ModuleRegistrationRow {
	var madService = Wire.auto[ModuleAndDepartmentService]

	var sprCode: String = null
	var sitsModuleCode: String = null
	var academicYear: AcademicYear = null
	var cats: String = null
	var assessmentGroup: String = null
	var selectionStatusCode: String = null

	var tabulaModuleCode = madService.getModuleByCode(sitsModuleCode).getOrElse(
			throw new ItemNotFoundException("No stem module for " + sitsModuleCode + " found in Tabula"))
		.code
}

object ModuleRegistrationImporter {

	val GetModuleRegistration = """
		select spr_code, mod_code, sms_mcrd, sms_agrp, ses_code, ayr_code
		from intuit.cam_sms sms, intuit.ins_stu stu, intuit.ins_spr spr
		where sms.spr_code = spr.spr_code
		and spr.spr_stuc = stu.stu_code
		and ayr_code = :academicYear and stu.stu_udf3 in (:usercodes)
		"""

	class ModuleRegistrationQuery(ds: DataSource)
		extends MappingSqlQuery[ImportModuleRegistrationsCommand](ds, GetModuleRegistration) {
		compile()
			override def mapRow(resultSet: ResultSet, rowNumber: Int) = {
				val modRegRow = new ModuleRegistrationRow
				modRegRow.sprCode = resultSet.getString("spr_code")
				modRegRow.sitsModuleCode = resultSet.getString("mod_code")
				modRegRow.cats = resultSet.getString("sms_mcrd")
				modRegRow.assessmentGroup = resultSet.getString("sms_agrp")
				modRegRow.selectionStatusCode = resultSet.getString("ses_code")
				modRegRow.academicYear = AcademicYear.parse(resultSet.getString("ayr_code")) // shouldn't need to parse this out of the result set, must be a better way ...

				new ImportModuleRegistrationsCommand(modRegRow)
		}
	}
}
