package uk.ac.warwick.tabula.services.scheduling

import java.sql.ResultSet

import javax.sql.DataSource
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Profiles.of
import org.springframework.core.env.{Environment, Profiles}
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.helpers.EnvironmentAwareness

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

trait SitsAcademicYearAware {
	var sitsAcademicYearService: SitsAcademicYearService = Wire[SitsAcademicYearService]

	def getCurrentSitsAcademicYearString: String = sitsAcademicYearService.getCurrentSitsAcademicYearString

	def getCurrentSitsAcademicYear: AcademicYear = {
		AcademicYear.parse(getCurrentSitsAcademicYearString)
	}
}

trait SitsAcademicYearService {
	def getCurrentSitsAcademicYearString: String
}

@Profile(Array("dev", "test", "production"))
@Service
class SitsAcademicYearServiceImpl extends SitsAcademicYearService with EnvironmentAwareness {
	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	private lazy val isDev = environment.acceptsProfiles(of("dev"))

	val GetCurrentAcademicYear = """
		select GET_AYR() ayr from dual
		"""

	def getCurrentSitsAcademicYearString: String =
		Option(new GetCurrentAcademicYearQuery(sits).execute().asScala.head)
  		.getOrElse {
				if (isDev) // Fall back for when SITS clone is being refreshed
					AcademicYear.now().toString()
				else
					throw new IllegalArgumentException("No current SITS academic year was available")
			}

	class GetCurrentAcademicYearQuery(ds: DataSource) extends MappingSqlQuery[String](ds, GetCurrentAcademicYear) {
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int): String = rs.getString("ayr")
	}
}

@Profile(Array("sandbox"))
@Service
class SandboxSitsAcademicYearService extends SitsAcademicYearService {

	def getCurrentSitsAcademicYearString: String =
		AcademicYear.now().toString()
}