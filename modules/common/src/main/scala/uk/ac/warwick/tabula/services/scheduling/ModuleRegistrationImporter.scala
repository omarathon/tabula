package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}
import javax.sql.DataSource

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportModuleRegistrationsCommand
import uk.ac.warwick.tabula.data.MemberDaoImpl
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.services.scheduling.ModuleRegistrationImporter.{AutoUploadedConfirmedModuleRegistrationsQuery, ConfirmedModuleRegistrationsQuery, UnconfirmedModuleRegistrationsQuery}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap

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
				queries.flatMap { query =>
					query.executeByNamedParam(params.asJava).asScala
				}.distinct.map { new ImportModuleRegistrationsCommand(_) }
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
				cats = new JBigDecimal(15),
				assessmentGroup = "A",
				selectionStatusCode = "C",
				occurrence = "A",
				academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now).toString,
				agreedMark = Some(new JBigDecimal("90.0")),
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

	def UnconfirmedModuleRegistrations = s"""
			select scj_code, sms.mod_code, sms.sms_mcrd as credit, sms.sms_agrp as assess_group,
			sms.ses_code, -- e.g. C for core or O for option
			sms.ayr_code, sms_occl as occurrence,
			null as smr_agrm, -- agreed mark
			null as smr_agrg -- agreed grade
				from $sitsSchema.ins_stu stu -- student
					join $sitsSchema.ins_spr spr -- Student Programme Route, needed for SPR code
						on spr.spr_stuc = stu.stu_code

					join $sitsSchema.srs_scj scj -- Student Course Join, needed for SCJ code
						on scj.scj_sprc = spr.spr_code

					join $sitsSchema.cam_sms sms -- Student Module Selection (unconfirmed module choices)
						on sms.spr_code = spr.spr_code

					join $sitsSchema.cam_ssn ssn -- holds student module registration status
						on sms.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = sms.ayr_code and ssn.ssn_mrgs != 'CON' -- module choices not confirmed
				where stu.stu_code = :universityId"""

	// The check on SMO_RTSC excludes WMG cancelled modules or module registrations
	def ConfirmedModuleRegistrations = s"""
			select scj_code, smo.mod_code, smo.smo_mcrd as credit, smo.smo_agrp as assess_group,
			smo.ses_code, smo.ayr_code, smo.mav_occur as occurrence,
			smr_agrm, -- agreed overall module mark
			smr_agrg -- agreed overall module grade
				from $sitsSchema.ins_stu stu
					join $sitsSchema.ins_spr spr
						on spr.spr_stuc = stu.stu_code

					join $sitsSchema.srs_scj scj
						on scj.scj_sprc = spr.spr_code

					join $sitsSchema.cam_smo smo
						on smo.spr_code = spr.spr_code
						and (smo_rtsc is null or (smo_rtsc not like 'X%' and smo_rtsc != 'Z')) -- exclude WMG cancelled registrations

					left join $sitsSchema.ins_smr smr -- Student Module Result
						on smo.spr_code = smr.spr_code
						and smo.ayr_code = smr.ayr_code
						and smo.mod_code = smr.mod_code
						and smo.mav_occur = smr.mav_occur

					join $sitsSchema.cam_ssn ssn
						on smo.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = smo.ayr_code and ssn.ssn_mrgs = 'CON'
				where stu.stu_code = :universityId"""

	// the left outer join to SSN excludes rows with a matching SSN since is only matching where SSN_SPRC is null
	// but that column has a non-null constraint
	def AutoUploadedConfirmedModuleRegistrations = s"""
			select scj_code, smo.mod_code, smo.smo_mcrd as credit, smo.smo_agrp as assess_group,
			smo.ses_code, smo.ayr_code, smo.mav_occur as occurrence, smr_agrm, smr_agrg
				from $sitsSchema.ins_stu stu
					join $sitsSchema.ins_spr spr
						on spr.spr_stuc = stu.stu_code

					join $sitsSchema.srs_scj scj
						on scj.scj_sprc = spr.spr_code

					join $sitsSchema.cam_smo smo
						on smo.spr_code = spr.spr_code
						and (smo_rtsc is null or (smo_rtsc not like 'X%' and smo_rtsc != 'Z'))

					left join $sitsSchema.ins_smr smr
						on smo.spr_code = smr.spr_code
						and smo.ayr_code = smr.ayr_code
						and smo.mod_code = smr.mod_code
						and smo.mav_occur = smr.mav_occur

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
	scjCode: String,
	sitsModuleCode: String,
	cats: JBigDecimal,
	assessmentGroup: String,
	selectionStatusCode: String,
	occurrence: String,
	academicYear: String,
	var agreedMark: Option[JBigDecimal],
	agreedGrade: String
)