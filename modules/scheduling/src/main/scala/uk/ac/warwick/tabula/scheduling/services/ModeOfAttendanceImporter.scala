package uk.ac.warwick.tabula.scheduling.services

import java.sql.ResultSet
import scala.collection.JavaConversions._
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service
import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSitsStatusCommand
import org.apache.log4j.Logger
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.ModeOfAttendance
import uk.ac.warwick.tabula.data.ModeOfAttendanceDao
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportModeOfAttendanceCommand
import org.springframework.context.annotation.Profile

/**
 * Provides access to modeofattendance data in SITS.
 *
 * (also provides access to the internal imported verison of data as well,
 *  so it's sort of a service too - bit of a mish mash of responsibility :|)
 */
trait ModeOfAttendanceImporter extends Logging {

	var modeOfAttendanceDao = Wire.auto[ModeOfAttendanceDao]

	var modeOfAttendanceMap: Map[String, ModeOfAttendance] = null

	def getModeOfAttendanceForCode(code: String): Option[ModeOfAttendance] = {
		if (modeOfAttendanceMap == null) {
			modeOfAttendanceMap = slurpModeOfAttendances()
		}
		modeOfAttendanceMap.get(code)
	}

	/** Get a list of commands that can be applied to save items to the modeofattendance table. */
	def getImportCommands: Seq[ImportModeOfAttendanceCommand]

	protected def slurpModeOfAttendances(): Map[String, ModeOfAttendance] = {
		transactional(readOnly = true) {
			logger.debug("refreshing SITS mode of attendance map")

			(for (modeOfAttendanceCode <- modeOfAttendanceDao.getAllStatusCodes; status <- modeOfAttendanceDao.getByCode(modeOfAttendanceCode)) yield {
				(modeOfAttendanceCode, status)
			}).toMap
		}
	}
}

@Profile(Array("dev", "test", "production"))
@Service
class ModeOfAttendanceImporterImpl extends ModeOfAttendanceImporter {
	import ModeOfAttendanceImporter._

	var sits = Wire[DataSource]("sitsDataSource")

	lazy val modeOfAttendanceQuery = new ModeOfAttendanceQuery(sits)

	def getImportCommands: Seq[ImportModeOfAttendanceCommand] = {
		val modeOfAttendances = modeOfAttendanceQuery.execute.toSeq
		// this slurp is always one behind, because the above query only selects and it doesn't
		// get inserted into our table until we return the result for the importer to apply.
		// but it isn't that important to be dead up to date with this data.
		modeOfAttendanceMap = slurpModeOfAttendances()
		modeOfAttendances
	}
}

@Profile(Array("sandbox"))
@Service
class SandboxModeOfAttendanceImporter extends ModeOfAttendanceImporter {
	def getImportCommands: Seq[ImportModeOfAttendanceCommand] =
		Seq(
			new ImportModeOfAttendanceCommand(ModeOfAttendanceInfo("F", "FULL-TIME", "Full-time according to Funding Council definitions")),
			new ImportModeOfAttendanceCommand(ModeOfAttendanceInfo("P", "PART-TIME", "Part-time"))
		)
}

case class ModeOfAttendanceInfo(code: String, shortName: String, fullName: String)

object ModeOfAttendanceImporter {
	val sitsSchema: String = Wire.property("${schema.sits}")

	val GetModeOfAttendance = f"""
		select moa_code, moa_snam, moa_name from $sitsSchema.ins_moa
		"""

	class ModeOfAttendanceQuery(ds: DataSource) extends MappingSqlQuery[ImportModeOfAttendanceCommand](ds, GetModeOfAttendance) {
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int) =
			new ImportModeOfAttendanceCommand(
				ModeOfAttendanceInfo(resultSet.getString("moa_code"), resultSet.getString("moa_snam"), resultSet.getString("moa_name"))
			)
	}

}
