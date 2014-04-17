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
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportAccreditedPriorLearningCommand
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import org.joda.time.DateTime
import java.math.BigDecimal

/**
 * Import accredited prior learning data from SITS.
 *
 */

trait AccreditedPriorLearningImporter {
	def getAccreditedPriorLearning(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportAccreditedPriorLearningCommand]
}

@Profile(Array("dev", "test", "production"))
@Service
class AccreditedPriorLearningImporterImpl extends AccreditedPriorLearningImporter with TaskBenchmarking {
	import AccreditedPriorLearningImporter._

	var sits = Wire[DataSource]("sitsDataSource")

	lazy val accreditedPriorLearningQuery = new AccreditedPriorLearningQuery(sits)

	def getAccreditedPriorLearningDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportAccreditedPriorLearningCommand] = {

		benchmarkTask("Fetch module registrations") {
			membersAndCategories.filter { _.member.userType == Student }.par.flatMap { mac =>
				val universityId = mac.member.universityId
				accreditedPriorLearningQuery.executeByNamedParam(Map("universityId" -> universityId))
			}.seq
		}
	}
}

@Profile(Array("sandbox")) @Service
class SandboxAccreditedPriorLearningImporter extends AccreditedPriorLearningImporter {
	var memberDao = Wire.auto[MemberDaoImpl]

	def getAccreditedPriorLearningDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportAccreditedPriorLearningCommand] =
		membersAndCategories flatMap { mac =>
			val universityId = mac.member.universityId
			val ssoUser = users(universityId)

			mac.member.userType match {
				case Student => studentAccreditedPriorLearningDetails(universityId, ssoUser)
				case _ => Seq()
			}
		}

	def studentAccreditedPriorLearningDetails(universityId: String, ssoUser: User) = {
		for {
			(code, d) <- SandboxData.Departments
			route <- d.routes.values.toSeq
			if (route.studentsStartId to route.studentsEndId).contains(universityId.toInt)
			moduleCode <- route.moduleCodes
		} yield {

			val row = AccreditedPriorLearningRow(
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

			new ImportAccreditedPriorLearningCommand(row)
		}
	}
}

object AccreditedPriorLearningImporter {
	val sitsSchema: String = Wire.property("${schema.sits}")

	val AccreditedPriorLearning = f"""
		select scj.scj_code, sac.awd_code, sac.sac_seq, sac.ayr_code, sac.sac_crdt, sac.lev_code, sac.sac_resn
		from $sitsSchema.ins_stu stu

		join $sitsSchema.ins_spr spr
			on spr.spr_stuc = stu.stu_code

		join $sitsSchema.cam_sac sac
			on sac.spr_code = spr.spr_code

	join $sitsSchema.srs_scj scj
		on (
			scj.scj_sprc = sac.spr_code
			and (
				-- choose the most significant course if there is one for this spr:
				scj.scj_udfa = 'Y'
				or (
					-- if there's no most signif scj for this spr, choose the scj with the max sequence number:
					scj.scj_seq2 = (
						select max(scj2.scj_seq2) from intuit.srs_scj scj2
							where scj2.scj_sprc = spr.spr_code
							and not exists (select * from intuit.srs_scj where scj_udfa in ('Y','y') and scj.scj_sprc = sac.spr_code)
					)
				)
			)
		)

		where stu.stu_code = :universityId"""

	def mapResultSet(resultSet: ResultSet): AccreditedPriorLearningRow = {
		var row = AccreditedPriorLearningRow(
			resultSet.getString("scj_code"),
			resultSet.getString("awd_code"),
			resultSet.getBigDecimal("sac_seq"),
			resultSet.getString("ayr_code"),
			resultSet.getString("ses_code"),
			resultSet.getString("occurrence"),
			resultSet.getString("ayr_code"),
			None,
			resultSet.getString("smr_agrg")
		)
		if (resultSet.getBigDecimal("smr_agrm") != null)
			row = AccreditedPriorLearningRow(
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

	class AccreditedPriorLearningQuery(ds: DataSource)
		extends MappingSqlQuery[AccreditedPriorLearningRow](ds, AccreditedPriorLearning) {
		declareParameter(new SqlParameter("universityId", Types.VARCHAR))
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int) = mapResultSet(resultSet)
	}
}

case class AccreditedPriorLearningRow(
																	...
																	) {
}
