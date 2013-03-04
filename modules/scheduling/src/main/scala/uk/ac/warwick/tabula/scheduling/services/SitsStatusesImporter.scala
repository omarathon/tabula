package uk.ac.warwick.tabula.scheduling.services

import java.sql.ResultSet
import java.sql.Types
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsJavaMap
import scala.util.matching.Regex
import org.apache.commons.lang3.text.WordUtils
import org.springframework.beans.factory.InitializingBean
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.SqlParameter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.Address
import uk.ac.warwick.tabula.data.model.AddressType
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.NextOfKin
import uk.ac.warwick.userlookup.User
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleMemberCommand
import uk.ac.warwick.tabula.data.model.MemberUserType._
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleStudentCommand
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleStaffCommand
import uk.ac.warwick.tabula.helpers.Logging
import org.springframework.remoting.rmi.RmiProxyFactoryBean
import collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.Department
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.data.model.Gender
import uk.ac.warwick.membership.MembershipInterfaceWrapper
import uk.ac.warwick.tabula.data.model.SitsStatus
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

