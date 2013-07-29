package uk.ac.warwick.tabula.services

import scala.collection.JavaConverters.asScalaBufferConverter

import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.junit.After
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model.Member

// scalastyle:off magic.number
class ProfileServiceTest extends AppContextTestBase with Mockito {

	@Autowired var relationshipService:RelationshipServiceImpl = _
	@Autowired var profileService:ProfileServiceImpl = _

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

		val dept1 = Fixtures.departmentWithId("in", "IT Services", "1")
		val dept2 = Fixtures.departmentWithId("cs", "Computing Science", "2")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)
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

}
