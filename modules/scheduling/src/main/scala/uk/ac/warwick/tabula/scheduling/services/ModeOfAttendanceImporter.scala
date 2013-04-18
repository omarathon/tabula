package uk.ac.warwick.tabula.scheduling.services

import java.sql.ResultSet
import scala.collection.JavaConversions._
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service
import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleSitsStatusCommand
import org.apache.log4j.Logger
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.ModeOfAttendance
import uk.ac.warwick.tabula.data.ModeOfAttendanceDao
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleModeOfAttendanceCommand

@Service
class ModeOfAttendanceImporter extends Logging {
	import ModeOfAttendanceImporter._

	var modeOfAttendanceDao = Wire[ModeOfAttendanceDao]
	
	var sits = Wire[DataSource]("sitsDataSource")
	
	lazy val modeOfAttendanceQuery = new ModeOfAttendanceQuery(sits)
	
	var modeOfAttendanceMap = slurpModeOfAttendances()

	def getModeOfAttendances(): Seq[ImportSingleModeOfAttendanceCommand] = {
		val modeOfAttendances = modeOfAttendanceQuery.execute.toSeq
		modeOfAttendanceMap = slurpModeOfAttendances()
		modeOfAttendances
	}
	
	def slurpModeOfAttendances(): Map[String, ModeOfAttendance] = {
		transactional(readOnly = true) {
			logger.debug("refreshing SITS mode of attendance map")

			(for (modeOfAttendanceCode <- modeOfAttendanceDao.getAllStatusCodes; status <- modeOfAttendanceDao.getByCode(modeOfAttendanceCode)) yield {
				(modeOfAttendanceCode, status)
			}).toMap
		}
	}		
}

object ModeOfAttendanceImporter {
		
	val GetModeOfAttendance = """
		select moa_code, moa_snam, moa_name from intuit.ins_moa
		"""
	
	class ModeOfAttendanceQuery(ds: DataSource) extends MappingSqlQuery[ImportSingleModeOfAttendanceCommand](ds, GetModeOfAttendance) {
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int) = new ImportSingleModeOfAttendanceCommand(resultSet)
	}
	
}
