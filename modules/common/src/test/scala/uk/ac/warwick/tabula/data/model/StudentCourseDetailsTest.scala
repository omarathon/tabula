package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConverters.asScalaBufferConverter

import org.hibernate.annotations.AccessType
import org.joda.time.LocalDate
import org.junit.runner.RunWith
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.{ActiveProfiles, ContextConfiguration}

import javax.persistence.{DiscriminatorValue, Entity, NamedQueries}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, PersistenceTestBase}
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}
import scala.collection.JavaConverters._

class StudentCourseDetailsTest extends PersistenceTestBase with Mockito {

	val profileService = mock[ProfileService]
	val relationshipService = mock[RelationshipService]

	@Test def getPersonalTutor {
		val student = new StudentMember

		val studentCourseDetails = new StudentCourseDetails(student, "0205225/1")
		studentCourseDetails.sprCode = "0205225/1"
		studentCourseDetails.relationshipService = relationshipService

		student.studentCourseDetails.add(studentCourseDetails)
		student.profileService = profileService

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		profileService.getStudentBySprCode("0205225/1") returns (Some(student))

		relationshipService.findCurrentRelationships(relationshipType, "0205225/1") returns (Nil)
		student.studentCourseDetails.get(0).relationships(relationshipType) should be ('empty)

		val rel = StudentRelationship("0672089", relationshipType, "0205225/1")
		rel.profileService = profileService

		relationshipService.findCurrentRelationships(relationshipType, "0205225/1") returns (Seq(rel))
		profileService.getMemberByUniversityId("0672089") returns (None)
		student.studentCourseDetails.get(0).relationships(relationshipType) map { _.agentParsed } should be (Seq("0672089"))

		val staff = Fixtures.staff(universityId="0672089")
		staff.firstName = "Steve"
		staff.lastName = "Taff"

		profileService.getMemberByUniversityId("0672089") returns (Some(staff))

		student.studentCourseDetails.get(0).relationships(relationshipType) map { _.agentParsed } should be (Seq(staff))
	}

	@Test def testModuleRegistrations {
		val member = new StudentMember
		member.universityId = "01234567"

		// create a student course details with module registrations
		val scd1 = new StudentCourseDetails(member, "2222222/2")
		member.studentCourseDetails.add(scd1)

		val mod1 = new Module
		val mod2 = new Module
		val modReg1 = new ModuleRegistration(scd1, mod1, new java.math.BigDecimal("12.0"), AcademicYear(2012), "A")
		val modReg2 = new ModuleRegistration(scd1, mod2, new java.math.BigDecimal("12.0"), AcademicYear(2013), "A")

		scd1.moduleRegistrations.add(modReg1)
		scd1.moduleRegistrations.add(modReg2)

		scd1.registeredModulesByYear(Some(AcademicYear(2013))) should be (Stream(mod2))
		scd1.registeredModulesByYear(None) should be (Stream(mod1, mod2))

		scd1.moduleRegistrations.asScala should be (Stream(modReg1, modReg2))
		scd1.moduleRegistrationsByYear(Some(AcademicYear(2012))) should be (Stream(modReg1))

	}

	@Test def testYearMethods {
		val stu = Fixtures.student()
		val scd = stu.mostSignificantCourse
		scd.beginDate = LocalDate.parse("2013-07-13")
		scd.endDate = scd.beginDate.plusYears(3)

		scd.beginYear should be ("2013")
		scd.endYear should be ("2016")

	}

}
