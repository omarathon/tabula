package uk.ac.warwick.courses.services

import uk.ac.warwick.courses.TestBase
import org.junit.Test
import uk.ac.warwick.courses.Mockito
import javax.sql.DataSource
import uk.ac.warwick.courses.helpers.ArrayList
import collection.JavaConversions._
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterUtils

class AssignmentImporterTest extends TestBase with Mockito {

	@Test def groupImportSql {
		// Not really testing AssignmentImporter but the behaviour of the query class for IN(..)
		// parameters. The SQL has to have the brackets, and the parameter value has to be a
		// Java List - a Scala collection will not be recognised and won't be expanded into multiple
		// question marks.
		val paramMap = Map(
				"module_code" -> "md101",
				"academic_year_code" -> ArrayList("10/11","11/12"),
				"mav_occurrence" -> "A",
				"assessment_group" -> "A"
		)
		val paramSource = new MapSqlParameterSource(paramMap)
		val sqlToUse = NamedParameterUtils.substituteNamedParameters(AssignmentImporter.GetAllAssessmentGroups, paramSource)
		sqlToUse.trim should endWith ("(?, ?)")
	}

}