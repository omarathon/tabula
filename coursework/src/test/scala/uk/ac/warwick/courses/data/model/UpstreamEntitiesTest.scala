package uk.ac.warwick.courses.data.model

import scala.collection.JavaConversions.seqAsJavaList

import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.junit.runner.RunWith
import org.junit.Test
import org.springframework.stereotype.Service
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.transaction.TransactionConfiguration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

import javax.persistence.Entity
import uk.ac.warwick.courses.services.AssignmentServiceImpl
import uk.ac.warwick.courses.AcademicYear
import uk.ac.warwick.courses.PersistenceTestBase

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
			
			for (entity <- Seq(law, law2010, law2011, law2012, group2010, group2011, otherGroup)) 
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
		}
	}
}