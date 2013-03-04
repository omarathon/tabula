package uk.ac.warwick.tabula.scheduling.services

import java.sql.ResultSet

import scala.collection.JavaConversions.asScalaBuffer

import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service

import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleSitsStatusCommand

@Service
class SitsStatusesImporter extends Logging {
	import SitsStatusesImporter._
	
	var sits = Wire[DataSource]("sitsDataSource")
	
	lazy val sitsStatusesQuery = new SitsStatusesQuery(sits)

	def getSitsStatuses(): Seq[ImportSingleSitsStatusCommand] = {
		sitsStatusesQuery.execute.toSeq
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

