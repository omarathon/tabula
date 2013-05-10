package uk.ac.warwick.tabula.scheduling.services

import scala.collection.JavaConversions.mapAsJavaMap

import org.junit.Test
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterUtils
import org.springframework.stereotype.Service

import javax.annotation.Resource
import javax.sql.DataSource
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.JavaImports._

// scalastyle:off magic.number
class AssignmentImporterTest extends PersistenceTestBase with Mockito {

	@Resource(name="academicDataStore") var ads:DataSource =_
	
	@Test def groupImportSql {
		// Not really testing AssignmentImporter but the behaviour of the query class for IN(..)
		// parameters. The SQL has to have the brackets, and the parameter value has to be a
		// Java List - a Scala collection will not be recognised and won't be expanded into multiple
		// question marks.
		val paramMap = Map(
				"module_code" -> "md101",
				"academic_year_code" -> JArrayList("10/11","11/12"),
				"mav_occurrence" -> "A",
				"assessment_group" -> "A"
		)
		val paramSource = new MapSqlParameterSource(paramMap)
		val sqlToUse = NamedParameterUtils.substituteNamedParameters(AssignmentImporter.GetAllAssessmentGroups, paramSource)
		sqlToUse.trim should endWith ("(?, ?)")
	}
	
	@Test def importMembers {
    withFakeTime(dateTime(2012, 5)) {

      val assignmentImporter = new AssignmentImporter
      assignmentImporter.ads = ads
      assignmentImporter.afterPropertiesSet

      var count = 0
      assignmentImporter.allMembers { mr =>
        count += 1
      }
      count should be (1)

    }
	}

}
