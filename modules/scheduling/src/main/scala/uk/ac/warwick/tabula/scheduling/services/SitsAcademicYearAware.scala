package uk.ac.warwick.tabula.scheduling.services

import org.springframework.jdbc.`object`.MappingSqlQuery
import java.sql.ResultSet
import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import scala.collection.JavaConversions._

trait SitsAcademicYearAware {
	var sits = Wire[DataSource]("sitsDataSource")

	val GetCurrentAcademicYear = """
		select UWTABS.GET_AYR() ayr from dual
		"""

	def getCurrentSitsAcademicYearString: String = {
		new GetCurrentAcademicYearQuery(sits).execute().head
	}

	def getCurrentSitsAcademicYear: AcademicYear = {
		AcademicYear.parse(getCurrentSitsAcademicYearString)
	}

	class GetCurrentAcademicYearQuery(ds: DataSource) extends MappingSqlQuery[String](ds, GetCurrentAcademicYear) {
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) = rs.getString("ayr")
	}

}