package uk.ac.warwick.tabula.services

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.joda.time.DateTimeUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberUserType._
import uk.ac.warwick.tabula.data.model.RelationshipType._
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.data.model.StudentRelationship

// scalastyle:off magic.number
class ProfileServiceTest extends AppContextTestBase with Mockito {

	@Autowired var relationshipService:RelationshipServiceImpl = _
	@Autowired var profileService:ProfileServiceImpl = _

	@Transactional
	@Test def findingRelationships = withFakeTime(dateTime(2000, 6)) {
		relationshipService.findCurrentRelationships(PersonalTutor, "1250148/1") should be ('empty)
		relationshipService.saveStudentRelationship(PersonalTutor, "1250148/1", "1234567")

		val opt = relationshipService.findCurrentRelationships(PersonalTutor, "1250148/1").headOption
		val currentRelationship = opt.getOrElse(fail("Failed to get current relationship"))
		currentRelationship.agent should be ("1234567")

		// now we've stored a relationship.  Try storing the identical one again:
		relationshipService.saveStudentRelationship(PersonalTutor, "1250148/1", "1234567")

		relationshipService.findCurrentRelationships(PersonalTutor, "1250148/1").headOption.getOrElse(fail("Failed to get current relationship after re-storing")).agent should be ("1234567")
		relationshipService.getRelationships(PersonalTutor, "1250148/1").size should be (1)

		// now store a new personal tutor for the same student. We should now have TWO personal tutors (as of TAB-416)
		DateTimeUtils.setCurrentMillisFixed(new DateTime().plusMillis(30).getMillis())
		relationshipService.saveStudentRelationship(PersonalTutor, "1250148/1", "7654321")

		val rels = relationshipService.getRelationships(PersonalTutor, "1250148/1")

		DateTimeUtils.setCurrentMillisFixed(new DateTime().plusMillis(30).getMillis())
		val currentRelationshipsUpdated = relationshipService.findCurrentRelationships(PersonalTutor, "1250148/1")
		currentRelationshipsUpdated.size should be (2)

		currentRelationshipsUpdated.find(_.agent == "7654321") should be ('defined)

		relationshipService.getRelationships(PersonalTutor, "1250148/1").size should be (2)

		relationshipService.listStudentRelationshipsWithUniversityId(PersonalTutor, "1234567").size should be (1)
		relationshipService.listStudentRelationshipsWithUniversityId(PersonalTutor, "7654321").size should be (1)
		relationshipService.listStudentRelationshipsWithUniversityId(PersonalTutor, "7654321").head.targetSprCode should be ("1250148/1")
	}

	@Before def setup: Unit = transactional { tx =>
		session.enableFilter(Member.ActiveOnlyFilter)
	}

	@After def tidyUp: Unit = transactional { tx =>
		session.disableFilter(Member.ActiveOnlyFilter)

		session.createCriteria(classOf[Member]).list().asInstanceOf[JList[Member]].asScala map { session.delete(_) }
	}

	@Test def crud = transactional { tx =>
		val m1 = Fixtures.student(universityId = "0000001", userId="student")
		val m2 = Fixtures.student(universityId = "0000002", userId="student")

		val m3 = Fixtures.staff(universityId = "0000003", userId="staff1")
		val m4 = Fixtures.staff(universityId = "0000004", userId="staff2")

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		profileService.save(m1)
		profileService.save(m2)

		profileService.getMemberByUniversityId("0000001") should be (Some(m1))
		profileService.getMemberByUniversityId("0000002") should be (Some(m2))
		profileService.getMemberByUniversityId("0000003") should be (Some(m3))
		profileService.getMemberByUniversityId("0000004") should be (Some(m4))
		profileService.getMemberByUniversityId("0000005") should be (None)

		profileService.getStudentBySprCode("0000001/1") should be (Some(m1))
		profileService.getStudentBySprCode("0000002/1") should be (Some(m2))
		profileService.getStudentBySprCode("0000003/1") should be (None)
		profileService.getStudentBySprCode("0000001/2") should be (None) // TODO This may not be the right behaviour

		session.enableFilter(Member.StudentsOnlyFilter)

		profileService.getMemberByUniversityId("0000003") should be (None)
		profileService.getMemberByUniversityId("0000004") should be (None)

		profileService.getAllMembersWithUserId("student", false) should be (Seq(m1, m2))
		profileService.getMemberByUserId("student", false) should be (Some(m1))
		profileService.getAllMembersWithUserId("student", true) should be (Seq(m1, m2))
		profileService.getAllMembersWithUserId("staff1", false) should be (Seq())
		profileService.getMemberByUserId("staff1", false) should be (None)
		profileService.getAllMembersWithUserId("staff1", true) should be (Seq(m3))
		profileService.getMemberByUserId("staff1", true) should be (Some(m3))
		profileService.getAllMembersWithUserId("unknown", false) should be (Seq())
		profileService.getAllMembersWithUserId("unknown", true) should be (Seq())

		session.disableFilter(Member.StudentsOnlyFilter)

		profileService.getAllMembersWithUserId("staff1", false) should be (Seq(m3))
		profileService.getMemberByUserId("staff1", false) should be (Some(m3))
	}

	@Test def listMembersUpdatedSince = transactional { tx =>
		session.flush()
		val dept1 = Fixtures.department("in", "IT Services")
		val dept2 = Fixtures.department("cs", "Computing Science")

		session.save(dept1)
		session.save(dept2)

		session.flush

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept1)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		session.flush

		profileService.listMembersUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 5) should be (Seq(m1, m2, m3, m4))
		profileService.listMembersUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 1) should be (Seq(m1))
		profileService.listMembersUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 0, 0, 0, 0), 5) should be (Seq(m2, m3, m4))
		profileService.listMembersUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), 5) should be (Seq())
	}

	@Test def getRegisteredModules: Unit = transactional { tx =>
		val mod1 = Fixtures.module("in101")
		val mod2 = Fixtures.module("in102")
		val mod3 = Fixtures.module("in103")

		session.save(mod1)
		session.save(mod2)
		session.save(mod3)

		val ua1 = Fixtures.upstreamAssignment("in", 1)
		val ua2 = Fixtures.upstreamAssignment("in", 2)

		// Check that we haven't changed the behaviour of Fixtures
		ua1.moduleCode should startWith (mod1.code.toUpperCase())
		ua2.moduleCode should startWith (mod2.code.toUpperCase())

		session.save(ua1)
		session.save(ua2)

		val ag1 = Fixtures.assessmentGroup(ua1)
		val ag2 = Fixtures.assessmentGroup(ua2)

		session.save(ag1)
		session.save(ag2)

		ag1.members.staticIncludeUsers.add("0000001")
		ag1.members.staticIncludeUsers.add("0000002")

		ag2.members.staticIncludeUsers.add("0000002")
		ag2.members.staticIncludeUsers.add("0000003")

		session.update(ag1)
		session.update(ag2)

		session.flush

		profileService.getRegisteredModules("0000001") should be (Seq(mod1))
		profileService.getRegisteredModules("0000002").toSet should be (Seq(mod1, mod2).toSet)
		profileService.getRegisteredModules("0000003") should be (Seq(mod2))
		profileService.getRegisteredModules("0000004") should be (Seq())
	}

	@Test def studentRelationships = transactional { tx =>
		val dept1 = Fixtures.department("in")
		val dept2 = Fixtures.department("cs")

		session.save(dept1)
		session.save(dept2)

		session.flush

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		m1.mostSignificantCourseDetails.get.sprCode = "1000001/1"
		m2.mostSignificantCourseDetails.get.sprCode = "1000002/1"

		profileService.save(m1)
		profileService.save(m2)

		val rel1 = relationshipService.saveStudentRelationship(RelationshipType.PersonalTutor, "0000001/1", "0000003")
		val rel2 = relationshipService.saveStudentRelationship(RelationshipType.PersonalTutor, "0000002/1", "0000003")

		session.save(rel1)
		session.save(rel2)

		relationshipService.findCurrentRelationships(RelationshipType.PersonalTutor, "0000001/1") should be (Seq(rel1))
		relationshipService.findCurrentRelationships(RelationshipType.PersonalTutor, "0000002/1") should be (Seq(rel2))
		relationshipService.findCurrentRelationships(RelationshipType.PersonalTutor, "0000003/1") should be (Nil)
		relationshipService.findCurrentRelationships(null, "0000001/1") should be (Nil)

		relationshipService.getRelationships(RelationshipType.PersonalTutor, "0000001/1") should be (Seq(rel1))
		relationshipService.getRelationships(RelationshipType.PersonalTutor, "0000002/1") should be (Seq(rel2))
		relationshipService.getRelationships(RelationshipType.PersonalTutor, "0000003/1") should be (Seq())
		relationshipService.getRelationships(null, "0000001/1") should be (Seq())

		relationshipService.listStudentRelationshipsByDepartment(RelationshipType.PersonalTutor, dept1) should be (Seq(rel1))
		relationshipService.listStudentRelationshipsByDepartment(RelationshipType.PersonalTutor, dept2) should be (Seq(rel2))
		relationshipService.listStudentRelationshipsByDepartment(null, dept1) should be (Seq())

		relationshipService.listStudentRelationshipsWithUniversityId(RelationshipType.PersonalTutor, "0000003").toSet should be (Seq(rel1, rel2).toSet)
		relationshipService.listStudentRelationshipsWithUniversityId(RelationshipType.PersonalTutor, "0000004") should be (Seq())
		relationshipService.listStudentRelationshipsWithUniversityId(null, "0000003") should be (Seq())

		val m5 = Fixtures.student(universityId = "1000005", userId="student", department=dept1)
		val m6 = Fixtures.student(universityId = "1000006", userId="student", department=dept2)

		m5.mostSignificantCourseDetails.get.sprCode = "1000005/1"
		m6.mostSignificantCourseDetails.get.sprCode = "1000006/1"

		profileService.save(m5)
		profileService.save(m6)

		relationshipService.listStudentsWithoutRelationship(RelationshipType.PersonalTutor, dept1) should be (Seq(m5))
		relationshipService.listStudentsWithoutRelationship(RelationshipType.PersonalTutor, dept2) should be (Seq(m6))
		relationshipService.listStudentsWithoutRelationship(null, dept1) should be (Seq())
	}
}
