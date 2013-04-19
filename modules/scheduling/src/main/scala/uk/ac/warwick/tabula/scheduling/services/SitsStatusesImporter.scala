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

@Service
class SitsStatusesImporter extends Logging {
	import SitsStatusesImporter._

	var sitsStatusDao = Wire[SitsStatusDao]
	
	var sits = Wire[DataSource]("sitsDataSource")
	
	lazy val sitsStatusesQuery = new SitsStatusesQuery(sits)
	
	var sitsStatusMap = slurpSitsStatuses()

	def getSitsStatuses(): Seq[ImportSingleSitsStatusCommand] = {
		val statuses = sitsStatusesQuery.execute.toSeq
		sitsStatusMap = slurpSitsStatuses()
		statuses
	}
	
	def slurpSitsStatuses(): Map[String, SitsStatus] = {
		transactional(readOnly = true) {
			logger.debug("refreshing SITS status map")

			(for (statusCode <- sitsStatusDao.getAllStatusCodes; status <- sitsStatusDao.getByCode(statusCode)) yield {
				(statusCode, status)
			}).toMap
		}
	}		
}

object SitsStatusesImporter {
		
	val GetSitsStatus = """
		select sta_code, sta_snam, sta_name from intuit.srs_sta
		"""
	
	class SitsStatusesQuery(ds: DataSource) extends MappingSqlQuery[ImportSingleSitsStatusCommand](ds, GetSitsStatus) {
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int) = new ImportSingleSitsStatusCommand(resultSet)
	}
	
}
