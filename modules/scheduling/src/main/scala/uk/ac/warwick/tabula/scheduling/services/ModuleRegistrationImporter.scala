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
import uk.ac.warwick.tabula.commands.TaskBenchmarking

/**
 * Import module registration data from SITS.
 *
 */

trait ModuleRegistrationImporter {
	def getModuleRegistrationDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportModuleRegistrationsCommand]
}

@Profile(Array("dev", "test", "production"))
@Service
class ModuleRegistrationImporterImpl extends ModuleRegistrationImporter with SitsAcademicYearAware with TaskBenchmarking {
	import ModuleRegistrationImporter._

	var sits = Wire[DataSource]("sitsDataSource")
	
	lazy val queries = Seq(
		new UnconfirmedModuleRegistrationsQuery(sits),
		new ConfirmedModuleRegistrationsQuery(sits),
		new AutoUploadedConfirmedModuleRegistrationsQuery(sits)
	)
	
	def getModuleRegistrationDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportModuleRegistrationsCommand] = {
		val sitsCurrentAcademicYear = getCurrentSitsAcademicYear

		benchmarkTask("Fetch module registrations") { 
			membersAndCategories.filter { _.member.userType == Student }.par.flatMap { mac =>
				val universityId = mac.member.universityId
				val params = HashMap(("year", sitsCurrentAcademicYear.toString), ("universityId", universityId))
				
				queries.flatMap { query => query.executeByNamedParam(params) }.distinct.map { new ImportModuleRegistrationsCommand(_, sitsCurrentAcademicYear) }
			}.seq
		}
	}
}

@Profile(Array("sandbox")) @Service
class SandboxModuleRegistrationImporter extends ModuleRegistrationImporter {
	var memberDao = Wire.auto[MemberDaoImpl]

	def getModuleRegistrationDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportModuleRegistrationsCommand] =
		membersAndCategories flatMap { mac =>
			val usercode = mac.member.usercode
			val ssoUser = users(mac.member.universityId)

			mac.member.userType match {
				case Student => studentModuleRegistrationDetails(usercode, ssoUser)
				case _ => Seq()
			}
		}

	def studentModuleRegistrationDetails(usercode: String, ssoUser: User) = {
		Seq()

		//  TODO: see code from sandbox assignment importer

	}
}

object ModuleRegistrationImporter {
	val sitsSchema: String = Wire.property("${schema.sits}")

	// union 3 things -
	// 1. unconfirmed module registrations from the SMS table
	// 2. confirmed module registrations from the SMO table where there is a module registration status of confirmed
	// 3. confirmed module registrations from the SMO table where no status is recorded, i.e. where MRs have been imported
	val UnconfirmedModuleRegistrations = f"""
			select scj_code, sms.mod_code, sms.sms_mcrd, sms.sms_agrp, sms.ses_code, sms.ayr_code, sms_occl as occurrence
				from $sitsSchema.ins_stu stu
					join $sitsSchema.ins_spr spr 
						on spr.spr_stuc = stu.stu_code
					
					join $sitsSchema.srs_scj scj 
						on scj.scj_sprc = spr.spr_code and scj.scj_udfa in ('Y','y')
					
					join $sitsSchema.cam_sms sms 
						on sms.spr_code = spr.spr_code and sms.ayr_code = :year
					
					join $sitsSchema.srs_vco vco 
						on vco.vco_crsc = scj.scj_crsc and vco.vco_rouc = spr.rou_code
					
					join $sitsSchema.cam_ssn ssn 
						on sms.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = sms.ayr_code and ssn.ssn_mrgs != 'CON'
				where stu.stu_code = :universityId"""
					
	val ConfirmedModuleRegistrations = f"""
			select scj_code, smo.mod_code, smo.smo_mcrd as sms_mcrd, smo.smo_agrp as sms_agrp, smo.ses_code, smo.ayr_code, smo.mav_occur as occurrence
				from $sitsSchema.ins_stu stu
					join $sitsSchema.ins_spr spr 
						on spr.spr_stuc = stu.stu_code
					
					join $sitsSchema.srs_scj scj 
						on scj.scj_sprc = spr.spr_code and scj.scj_udfa in ('Y','y')
					
					join $sitsSchema.cam_smo smo 
						on smo.spr_code = spr.spr_code and smo.ayr_code = :year
					
					join $sitsSchema.srs_vco vco 
						on vco.vco_crsc = scj.scj_crsc and vco.vco_rouc = spr.rou_code
					
					join $sitsSchema.cam_ssn ssn 
						on smo.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = smo.ayr_code and ssn.ssn_mrgs = 'CON'
				where stu.stu_code = :universityId"""
					
	val AutoUploadedConfirmedModuleRegistrations = f"""
			select scj_code, smo.mod_code, smo.smo_mcrd as sms_mcrd, smo.smo_agrp as sms_agrp, smo.ses_code, smo.ayr_code, smo.mav_occur as occurrence
				from $sitsSchema.ins_stu stu
					join $sitsSchema.ins_spr spr 
						on spr.spr_stuc = stu.stu_code
					
					join $sitsSchema.srs_scj scj 
						on scj.scj_sprc = spr.spr_code and scj.scj_udfa in ('Y','y')
					
					join $sitsSchema.cam_smo smo 
						on smo.spr_code = spr.spr_code and smo.ayr_code = :year
					
					join $sitsSchema.srs_vco vco 
						on vco.vco_crsc = scj.scj_crsc and vco.vco_rouc = spr.rou_code
					
					left outer join $sitsSchema.cam_ssn ssn 
						on smo.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = smo.ayr_code
				where stu.stu_code = :universityId
					and ssn.ssn_sprc is null
			"""
						
	def mapResultSet(resultSet: ResultSet) = 
		ModuleRegistrationRow(
			resultSet.getString("scj_code"),
			resultSet.getString("mod_code"),
			resultSet.getBigDecimal("sms_mcrd"),
			resultSet.getString("sms_agrp"),
			resultSet.getString("ses_code"),
			resultSet.getString("occurrence")
		)

	class UnconfirmedModuleRegistrationsQuery(ds: DataSource)
		extends MappingSqlQuery[ModuleRegistrationRow](ds, UnconfirmedModuleRegistrations) {
			declareParameter(new SqlParameter("universityId", Types.VARCHAR))
			declareParameter(new SqlParameter("year", Types.VARCHAR))
			compile()
			override def mapRow(resultSet: ResultSet, rowNumber: Int) = mapResultSet(resultSet)
	}
	
	class ConfirmedModuleRegistrationsQuery(ds: DataSource)
		extends MappingSqlQuery[ModuleRegistrationRow](ds, ConfirmedModuleRegistrations) {
			declareParameter(new SqlParameter("universityId", Types.VARCHAR))
			declareParameter(new SqlParameter("year", Types.VARCHAR))
			compile()
			override def mapRow(resultSet: ResultSet, rowNumber: Int) = mapResultSet(resultSet)
	}
	
	class AutoUploadedConfirmedModuleRegistrationsQuery(ds: DataSource)
		extends MappingSqlQuery[ModuleRegistrationRow](ds, AutoUploadedConfirmedModuleRegistrations) {
			declareParameter(new SqlParameter("universityId", Types.VARCHAR))
			declareParameter(new SqlParameter("year", Types.VARCHAR))
			compile()
			override def mapRow(resultSet: ResultSet, rowNumber: Int) = mapResultSet(resultSet)
	}
}

case class ModuleRegistrationRow(
	val scjCode: String,
	val sitsModuleCode: String,
	val cats: java.math.BigDecimal,
	val assessmentGroup: String,
	val selectionStatusCode: String,
	val occurrence: String)
