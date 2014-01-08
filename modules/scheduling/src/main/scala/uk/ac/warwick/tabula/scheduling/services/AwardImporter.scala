package uk.ac.warwick.tabula.scheduling.services

import java.sql.ResultSet

import scala.Option.option2Iterable
import scala.collection.JavaConversions.asScalaBuffer

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service

import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.AwardDao
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Award
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportAwardCommand

trait AwardImporter extends Logging {
	var awardDao = Wire[AwardDao]

	private var awardMap: Map[String, Award] = _

	def getAwardForCode(code: String) = {
		if (awardMap == null) updateAwardMap()
		awardMap(code)
	}

	protected def updateAwardMap() {
		awardMap = slurpAwards()
	}

	private def slurpAwards(): Map[String, Award] = {
		transactional(readOnly = true) {
			logger.debug("refreshing SITS award map")

			(for {
				awardCode <- awardDao.getAllAwardCodes
				award <- awardDao.getByCode(awardCode)
			} yield (awardCode -> award)).toMap

		}
	}

	def importAwards() {
		logger.info("Importing awards")

		getImportCommands foreach { _.apply() }

		updateAwardMap()
	}

	def getImportCommands: Seq[ImportAwardCommand]
}

@Profile(Array("dev", "test", "production")) @Service
class SitsAwardImporter extends AwardImporter {
	import SitsAwardImporter._

	var sits = Wire[DataSource]("sitsDataSource")

	lazy val awardsQuery = new AwardsQuery(sits)

	def getImportCommands: Seq[ImportAwardCommand] = {
		awardsQuery.execute.toSeq
	}
}

object SitsAwardImporter {

	val GetAward = """
		select awd_code, awd_snam, awd_name from ins_awd
		"""

	class AwardsQuery(ds: DataSource) extends MappingSqlQuery[ImportAwardCommand](ds, GetAward) {
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int) =
			new ImportAwardCommand(
				AwardInfo(
					code=resultSet.getString("awd_code"),
					shortName=resultSet.getString("awd_snam"),
					fullName=resultSet.getString("awd_name")
				)
			)
	}

}

case class AwardInfo(code: String, shortName: String, fullName: String)

@Profile(Array("sandbox")) @Service
class SandboxAwardImporter extends AwardImporter {

	def getImportCommands: Seq[ImportAwardCommand] =
		Seq(
			// the most common awards for current students:
			new ImportAwardCommand(AwardInfo("BSC", "BSC (HONS)", "Bachelor of Science (with Honours)")),
			new ImportAwardCommand(AwardInfo("BA", "BA (HONS)", "Bachelor of Arts (with Honours)")),
			new ImportAwardCommand(AwardInfo("NO QUAL PG", "NO QUAL PG", "No qualification aimed for")),
			new ImportAwardCommand(AwardInfo("MSC", "MSC", "Master of Science")),
			new ImportAwardCommand(AwardInfo("MBA", "MBA", "Master of Business Administration")),
			new ImportAwardCommand(AwardInfo("PHD", "PhD", "Doctor of Philosophy")),
			new ImportAwardCommand(AwardInfo("MA", "MA", "Master of Arts")),
			new ImportAwardCommand(AwardInfo("PGCERT", "PG Cert", "Postgraduate Certificate"))
		)
}
