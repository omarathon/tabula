package uk.ac.warwick.tabula.scheduling.services

import java.sql.ResultSet

import scala.collection.JavaConversions._

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service

import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleSitsStatusCommand

trait SitsStatusesImporter extends Logging {
	var sitsStatusDao = Wire.auto[SitsStatusDao]
	
	var sitsStatusMap = slurpSitsStatuses()
	
	def getSitsStatuses: Seq[ImportSingleSitsStatusCommand]
	
	def slurpSitsStatuses(): Map[String, SitsStatus] = {
		transactional(readOnly = true) {
			logger.debug("refreshing SITS status map")

			(for (statusCode <- sitsStatusDao.getAllStatusCodes; status <- sitsStatusDao.getByCode(statusCode)) yield {
				(statusCode, status)
			}).toMap
		}
	}	
}

@Profile(Array("dev", "test", "production")) @Service
class SitsStatusesImporterImpl extends SitsStatusesImporter {
	import SitsStatusesImporter._
	
	var sits = Wire[DataSource]("sitsDataSource")
	
	lazy val sitsStatusesQuery = new SitsStatusesQuery(sits)

	def getSitsStatuses: Seq[ImportSingleSitsStatusCommand] = {
		val statuses = sitsStatusesQuery.execute.toSeq
		sitsStatusMap = slurpSitsStatuses()
		statuses
	}	
}

@Profile(Array("sandbox")) @Service
class SandboxSitsStatusesImporter extends SitsStatusesImporter {
	def getSitsStatuses: Seq[ImportSingleSitsStatusCommand] = 
		Seq(
			new ImportSingleSitsStatusCommand(SitsStatusInfo("C", "CURRENT STUDENT", "Current Student")),
			new ImportSingleSitsStatusCommand(SitsStatusInfo("P", "PERMANENTLY W/D", "Permanently Withdrawn"))
		)
}

case class SitsStatusInfo(code: String, shortName: String, fullName: String)

object SitsStatusesImporter {
		
	val GetSitsStatus = """
		select sta_code, sta_snam, sta_name from intuit.srs_sta
		"""
	
	class SitsStatusesQuery(ds: DataSource) extends MappingSqlQuery[ImportSingleSitsStatusCommand](ds, GetSitsStatus) {
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int) = 
			new ImportSingleSitsStatusCommand(
				SitsStatusInfo(resultSet.getString("sta_code"), resultSet.getString("sta_snam"), resultSet.getString("sta_name"))
			)
	}
	
}
