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
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.MemberDaoImpl
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportModuleRegistrationsCommand
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import org.joda.time.DateTime
import java.math.BigDecimal

/**
 * Import module registration data from SITS.
 *
 */

trait ModuleRegistrationImporter {
	def getModuleRegistrationDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportModuleRegistrationsCommand]
}

@Profile(Array("dev", "test", "production"))
@Service
class ModuleRegistrationImporterImpl extends ModuleRegistrationImporter with TaskBenchmarking {
	import ModuleRegistrationImporter._

	var sits = Wire[DataSource]("sitsDataSource")
	
	lazy val queries = Seq(
		new UnconfirmedModuleRegistrationsQuery(sits),
		new ConfirmedModuleRegistrationsQuery(sits),
		new AutoUploadedConfirmedModuleRegistrationsQuery(sits)
	)
	
	def getModuleRegistrationDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportModuleRegistrationsCommand] = {

		benchmarkTask("Fetch module registrations") { 
			membersAndCategories.filter { _.member.userType == Student }.par.flatMap { mac =>
				val universityId = mac.member.universityId
				val params = HashMap(("universityId", universityId))
				logger.debug(f"getting module registrations for $universityId")
				queries.flatMap { query => query.executeByNamedParam(params) }.distinct.map { new ImportModuleRegistrationsCommand(_) }
			}.seq
		}
	}
}

@Profile(Array("sandbox")) @Service
class SandboxModuleRegistrationImporter extends ModuleRegistrationImporter {
	var memberDao = Wire.auto[MemberDaoImpl]

	def getModuleRegistrationDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportModuleRegistrationsCommand] =
		membersAndCategories flatMap { mac =>
			val universityId = mac.member.universityId
			val ssoUser = users(universityId)

			mac.member.userType match {
				case Student => studentModuleRegistrationDetails(universityId, ssoUser)
				case _ => Seq()
			}
		}

	def studentModuleRegistrationDetails(universityId: String, ssoUser: User) = {
		for {
			(code, d) <- SandboxData.Departments
			route <- d.routes.values.toSeq
			if (route.studentsStartId to route.studentsEndId).contains(universityId.toInt)
			moduleCode <- route.moduleCodes
		} yield {

			val row = ModuleRegistrationRow(
				scjCode = "%s/1".format(universityId),
				sitsModuleCode = "%s-15".format(moduleCode.toUpperCase),
				cats = new java.math.BigDecimal(15),
				assessmentGroup = "A",
				selectionStatusCode = "C",
				occurrence = "A",
				academicYear = AcademicYear.guessByDate(DateTime.now).toString,
				agreedMark = Some(new java.math.BigDecimal("90.0")),
				agreedGrade = "A"
			)

			new ImportModuleRegistrationsCommand(row)
		}
	}
}

object ModuleRegistrationImporter {
	val sitsSchema: String = Wire.property("${schema.sits}")

	// union 3 things -
	// 1. unconfirmed module registrations from the SMS table
	// 2. confirmed module registrations from the SMO table where there is a module registration status of confirmed
	// 3. confirmed module registrations from the SMO table where no status is recorded, i.e. where MRs have been imported
	//
	// the 3 queries should be mutually exclusive - 1st has SSN_MRGS != CON, 2nd has SSN_MRGS == CON and 3rd has no SSN.
	//
	// Although the 3 queries aren't unioned in SQL now, the column names still need to match.

	val UnconfirmedModuleRegistrations = f"""
			select scj_code, sms.mod_code, sms.sms_mcrd as credit, sms.sms_agrp as assess_group,
			sms.ses_code, sms.ayr_code, sms_occl as occurrence, null as smr_agrm, null as smr_agrg
				from $sitsSchema.ins_stu stu
					join $sitsSchema.ins_spr spr 
						on spr.spr_stuc = stu.stu_code
					
					join $sitsSchema.srs_scj scj 
						on scj.scj_sprc = spr.spr_code and scj.scj_udfa in ('Y','y')
					
					join $sitsSchema.cam_sms sms 
						on sms.spr_code = spr.spr_code
					
					join $sitsSchema.srs_vco vco 
						on vco.vco_crsc = scj.scj_crsc and vco.vco_rouc = spr.rou_code
					
					join $sitsSchema.cam_ssn ssn 
						on sms.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = sms.ayr_code and ssn.ssn_mrgs != 'CON'
				where stu.stu_code = :universityId"""
					
	val ConfirmedModuleRegistrations = f"""
			select scj_code, smo.mod_code, smo.smo_mcrd as credit, smo.smo_agrp as assess_group,
			smo.ses_code, smo.ayr_code, smo.mav_occur as occurrence, smr_agrm, smr_agrg
				from $sitsSchema.ins_stu stu
					join $sitsSchema.ins_spr spr 
						on spr.spr_stuc = stu.stu_code
					
					join $sitsSchema.srs_scj scj 
						on scj.scj_sprc = spr.spr_code and scj.scj_udfa in ('Y','y')
					
					join $sitsSchema.cam_smo smo 
						on smo.spr_code = spr.spr_code
					
					join $sitsSchema.srs_vco vco 
						on vco.vco_crsc = scj.scj_crsc and vco.vco_rouc = spr.rou_code

					join $sitsSchema.ins_smr smr
						on smo.spr_code = smr.spr_code
						and smo.ayr_code = smr.ayr_code
						and smo.mod_code = smr.mod_code
					
					join $sitsSchema.cam_ssn ssn 
						on smo.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = smo.ayr_code and ssn.ssn_mrgs = 'CON'
				where stu.stu_code = :universityId"""

	// the left outer join to SSN excludes rows with a matching SSN since is only matching where SSN_SPRC is null
	// but that column has a non-null constraint
	val AutoUploadedConfirmedModuleRegistrations = f"""
			select scj_code, smo.mod_code, smo.smo_mcrd as credit, smo.smo_agrp as assess_group,
			smo.ses_code, smo.ayr_code, smo.mav_occur as occurrence, smr_agrm, smr_agrg
				from $sitsSchema.ins_stu stu
					join $sitsSchema.ins_spr spr 
						on spr.spr_stuc = stu.stu_code
					
					join $sitsSchema.srs_scj scj 
						on scj.scj_sprc = spr.spr_code and scj.scj_udfa in ('Y','y')
					
					join $sitsSchema.cam_smo smo 
						on smo.spr_code = spr.spr_code
					
					join $sitsSchema.srs_vco vco 
						on vco.vco_crsc = scj.scj_crsc and vco.vco_rouc = spr.rou_code

					join $sitsSchema.ins_smr smr
						on smo.spr_code = smr.spr_code
						and smo.ayr_code = smr.ayr_code
						and smo.mod_code = smr.mod_code

					left outer join $sitsSchema.cam_ssn ssn
						on smo.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = smo.ayr_code
				where stu.stu_code = :universityId
					and ssn.ssn_sprc is null
			"""
						
	def mapResultSet(resultSet: ResultSet): ModuleRegistrationRow = {
		var row = ModuleRegistrationRow(
			resultSet.getString("scj_code"),
			resultSet.getString("mod_code"),
			resultSet.getBigDecimal("credit"),
			resultSet.getString("assess_group"),
			resultSet.getString("ses_code"),
			resultSet.getString("occurrence"),
			resultSet.getString("ayr_code"),
			None,
			resultSet.getString("smr_agrg")
		)
		if (resultSet.getBigDecimal("smr_agrm") != null)
			row = ModuleRegistrationRow(
				resultSet.getString("scj_code"),
				resultSet.getString("mod_code"),
				resultSet.getBigDecimal("credit"),
				resultSet.getString("assess_group"),
				resultSet.getString("ses_code"),
				resultSet.getString("occurrence"),
				resultSet.getString("ayr_code"),
				Some(resultSet.getBigDecimal("smr_agrm")),
				resultSet.getString("smr_agrg"))
		row
	}

	class UnconfirmedModuleRegistrationsQuery(ds: DataSource)
		extends MappingSqlQuery[ModuleRegistrationRow](ds, UnconfirmedModuleRegistrations) {
			declareParameter(new SqlParameter("universityId", Types.VARCHAR))
			compile()
			override def mapRow(resultSet: ResultSet, rowNumber: Int) = mapResultSet(resultSet)
	}
	
	class ConfirmedModuleRegistrationsQuery(ds: DataSource)
		extends MappingSqlQuery[ModuleRegistrationRow](ds, ConfirmedModuleRegistrations) {
			declareParameter(new SqlParameter("universityId", Types.VARCHAR))
			compile()
			override def mapRow(resultSet: ResultSet, rowNumber: Int) = mapResultSet(resultSet)
	}
	
	class AutoUploadedConfirmedModuleRegistrationsQuery(ds: DataSource)
		extends MappingSqlQuery[ModuleRegistrationRow](ds, AutoUploadedConfirmedModuleRegistrations) {
			declareParameter(new SqlParameter("universityId", Types.VARCHAR))
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
	val occurrence: String,
	val academicYear: String,
	var agreedMark: Option[java.math.BigDecimal],
	val agreedGrade: String
) {
}
