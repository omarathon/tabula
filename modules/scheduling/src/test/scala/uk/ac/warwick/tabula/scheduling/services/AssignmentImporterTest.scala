package uk.ac.warwick.tabula.scheduling.services

import javax.sql.DataSource

import scala.collection.JavaConverters._
import scala.reflect._

import org.junit.{After, Test}
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterUtils
import org.springframework.stereotype.Service
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder

import uk.ac.warwick.tabula.{TestBase, Mockito, PersistenceTestBase}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.data.model.AssessmentComponent

trait EmbeddedAds {
	val ads = new EmbeddedDatabaseBuilder().addScript("ads.sql").build()

	@After def after {
		ads.shutdown()
	}
}

// scalastyle:off magic.number
class AssignmentImporterTest extends TestBase with Mockito with EmbeddedAds {

	val assignmentImporter = new AssignmentImporterImpl
	assignmentImporter.ads = ads
	assignmentImporter.afterPropertiesSet

	val NONE = AssessmentComponent.NoneAssessmentGroup
	
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
		val paramSource = new MapSqlParameterSource(paramMap.asJava)
		val sqlToUse = NamedParameterUtils.substituteNamedParameters(AssignmentImporter.GetAllAssessmentGroups, paramSource)
		sqlToUse.trim should endWith ("(?, ?)")
	}
	
	@Test def importMembers { withFakeTime(dateTime(2012, 5)) {
		var count = 0
		assignmentImporter.allMembers { mr =>
			count += 1
		}
		count should be (4)	
	}}

	@Test def emptyAssessmentGroups { withFakeTime(dateTime(2012, 5)) {
		val emptyGroups = assignmentImporter.getEmptyAssessmentGroups
		emptyGroups.size should be (2)
		emptyGroups should containMatching[UpstreamAssessmentGroup] ( _.moduleCode == "CH130-15" )
		emptyGroups should containMatching[UpstreamAssessmentGroup] ( _.moduleCode == "CH130-20" )
	}}

	@Test def getAllAssessmentGroups { withFakeTime(dateTime(2012, 5)) {
		val allGroups = sorted(assignmentImporter.getAllAssessmentGroups)
		val tuples = allGroups map asTuple

		/* We currently get the NONE assessmentgroups even for groups
		   that aren't empty. We do only generate AssessmentComponents
		   when we need to, so these groups shouldnt' appear in the UI
		   unnecessarily - but we could change this query to filter it
		   down a bit more. */
		tuples should be (Seq(
			("CH115-30","A","A"),
			("CH115-30",NONE,NONE),
			("CH120-15","A","A"),
			("CH120-15",NONE,NONE),
			("CH130-15","A","A"),
			("CH130-15",NONE,NONE),
			("CH130-20","A","A"),
			("CH130-20",NONE,NONE)
		))

	}}

	@Test def getAllAssessmentComponents { withFakeTime(dateTime(2012, 5)) {
		val components = sorted(assignmentImporter.getAllAssessmentComponents)
		val tuples = components map asTuple
		
		tuples should be (Seq(
			("CH115-30","A","Chemicals Essay"), 
			("CH120-15","A","Chemistry Dissertation"), 
			("CH130-15","A","Chem 130 A01"), 
			("CH130-20","A","Chem 130 A01 (20 CATS)")
		))

	}}

	private def asTuple(component: AssessmentComponent) = (component.moduleCode, component.assessmentGroup, component.name)
	private def sorted(components: Seq[AssessmentComponent])(implicit ev: ClassTag[AssessmentComponent]) = components sortBy asTuple

	// Converting to tuples for easier sorting/comparing
	private def asTuple(group: UpstreamAssessmentGroup) = (group.moduleCode, group.assessmentGroup, group.occurrence)
	private def sorted(groups: Seq[UpstreamAssessmentGroup]) = groups sortBy asTuple

}
