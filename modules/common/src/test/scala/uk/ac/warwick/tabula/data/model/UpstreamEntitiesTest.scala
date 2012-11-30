package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConversions.seqAsJavaList
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.junit.runner.RunWith
import org.junit.Test
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import javax.persistence.Entity
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.PersistenceTestBase
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import uk.ac.warwick.tabula.services.AssignmentServiceImpl

class UpstreamEntitiesTest extends PersistenceTestBase {
	@Test def associations {
		transactional { t =>
			val law = new UpstreamAssignment
			law.moduleCode = "la155-10"
			law.assessmentGroup = "A"
			law.sequence = "A02"
			law.departmentCode = "la"
			law.name = "Cool Essay"
				
			val law2010 = new Assignment
			law2010.name = "Cool Essay!"
			law2010.academicYear = new AcademicYear(2010)
			law2010.upstreamAssignment = law
			law2010.occurrence = "A"
			
			val law2011 = new Assignment
			law2011.name = "Cool Essay?"
			law2011.academicYear = new AcademicYear(2011)
			law2011.upstreamAssignment = law
			law2011.occurrence = "A"
				
			// Not linked to an upstream assignment
			val law2012 = new Assignment
			law2012.name = "Cool Essay?"
			law2012.academicYear = new AcademicYear(2011)
			
			val group2010 = new UpstreamAssessmentGroup
			group2010.moduleCode = "la155-10"
			group2010.occurrence = "A"
			group2010.assessmentGroup = "A"
			group2010.academicYear = new AcademicYear(2010)
			group2010.members.staticIncludeUsers.addAll(Seq("rob","kev","bib"))
			
			val group2011 = new UpstreamAssessmentGroup
			group2011.moduleCode = "la155-10"
			group2011.occurrence = "A"
			group2011.assessmentGroup = "A"
			group2011.academicYear = new AcademicYear(2011)
			group2011.members.staticIncludeUsers.addAll(Seq("hog","dod","han"))
			
			// similar group but doesn't match the occurence of any assignment above, so ignored.
			val otherGroup = new UpstreamAssessmentGroup
			otherGroup.moduleCode = "la155-10"
			otherGroup.occurrence = "B"
			otherGroup.assessmentGroup = "A"
			otherGroup.academicYear = new AcademicYear(2011)
			otherGroup.members.staticIncludeUsers.addAll(Seq("hog","dod","han"))
			
			val member = new Member
			member.universityId = "0672089"
			member.userId = "cuscav"					
			member.firstName = "Mathew"
			member.lastName = "Mannion"
			member.email = "M.Mannion@warwick.ac.uk"
			
			for (entity <- Seq(law, law2010, law2011, law2012, group2010, group2011, otherGroup, member)) 
				session.save(entity)
			session.flush
			session.clear
			
			val dao = new AssignmentServiceImpl
			dao.sessionFactory = sessionFactory
			
			dao.getAssessmentGroup(law2010).map { group =>
				group.id should be (group2010.id)
				group.members.includes("bib") should be (true)
			}.orElse(fail)
			
			dao.getAssessmentGroup(law2011).map { group =>
				group.id should be (group2011.id)
				group.members.includes("dod") should be (true)
			}.orElse(fail)
			
			dao.getAssessmentGroup(law2012).isDefined should be (false)
			
			session.load(classOf[Member], "0672089") match {
				case loadedMember:Member => loadedMember.firstName should be ("Mathew")
				case _ => fail("Department not found")
			}
		}
	}
}