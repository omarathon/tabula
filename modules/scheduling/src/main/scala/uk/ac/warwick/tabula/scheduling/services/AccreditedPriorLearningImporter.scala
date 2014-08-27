package uk.ac.warwick.tabula.scheduling.services

import java.sql.ResultSet
import java.sql.Types
import scala.collection.JavaConversions._
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

	def getAccreditedPriorLearning(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]):
		Seq[ImportAccreditedPriorLearningCommand] = {

		benchmarkTask("Fetch accredited prior learning") {
			membersAndCategories.filter { _.member.userType == Student }.par.flatMap { mac =>
				val universityId = mac.member.universityId
				accreditedPriorLearningQuery.executeByNamedParam(Map("universityId" -> universityId)).toSeq.map(row => new ImportAccreditedPriorLearningCommand(row))
			}.seq
		}
	}
}

@Profile(Array("sandbox")) @Service
class SandboxAccreditedPriorLearningImporter extends AccreditedPriorLearningImporter {
	var memberDao = Wire.auto[MemberDaoImpl]

	def getAccreditedPriorLearning(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportAccreditedPriorLearningCommand] =
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
				awardCode = "BA",
				sequenceNumber = 1,
				academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now).toString,
				cats = new java.math.BigDecimal(15),
				levelCode = "2",
				reason = "Exemption of 30 CATS for 3 terms of Open Studies Languages"
			)

			new ImportAccreditedPriorLearningCommand(row)
		}
	}
}

object AccreditedPriorLearningImporter {
	val sitsSchema: String = Wire.property("${schema.sits}")

	// Choose the most significant course if there is one for this SPR.
	// If there is no most signif SCJ for this SPR, choose the SCJ with the max sequence number.
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
				scj.scj_udfa = 'Y'
				or (
					scj.scj_seq2 = (
						select max(scj2.scj_seq2) from $sitsSchema.srs_scj scj2
							where scj2.scj_sprc = spr.spr_code
							and not exists (select * from $sitsSchema.srs_scj where scj_udfa in ('Y','y') and scj.scj_sprc = sac.spr_code)
					)
				)
			)
		)

		where stu.stu_code = :universityId and sac.ayr_code is not null"""


	def mapResultSet(resultSet: ResultSet): AccreditedPriorLearningRow = {
		new AccreditedPriorLearningRow(resultSet.getString("scj_code"),
		resultSet.getString("awd_code"),
		resultSet.getInt("sac_seq"),
		resultSet.getString("ayr_code"),
		resultSet.getBigDecimal("sac_crdt"),
		resultSet.getString("lev_code"),
		resultSet.getString("sac_resn"))
	}

	class AccreditedPriorLearningQuery(ds: DataSource)
		extends MappingSqlQuery[AccreditedPriorLearningRow](ds, AccreditedPriorLearning) {
		declareParameter(new SqlParameter("universityId", Types.VARCHAR))
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int) = mapResultSet(resultSet)
	}
}

case class AccreditedPriorLearningRow(
	val scjCode: String,
	val awardCode: String,
	val sequenceNumber: Int,
	val academicYear: String,
	val cats: java.math.BigDecimal,
	val levelCode: String,
	val reason: String) {}
