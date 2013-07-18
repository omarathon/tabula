package uk.ac.warwick.tabula.services

import scala.collection.JavaConverters.asScalaBufferConverter

import org.hibernate.annotations.AccessType
import org.hibernate.annotations.FilterDefs
import org.hibernate.annotations.Filters
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.joda.time.DateTimeUtils
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional

import javax.persistence.DiscriminatorColumn
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.Inheritance
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor

// scalastyle:off magic.number
class RelationshipServiceTest extends AppContextTestBase with Mockito {

	@Autowired var relationshipService: RelationshipServiceImpl = _
	@Autowired var profileService: ProfileServiceImpl = _

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

	@Test def saveFindGetStudentRelationships = transactional { tx =>

		val dept1 = Fixtures.department("el", "Equatorial Life")
		val dept2 = Fixtures.department("nr", "Northern Regions")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)
		session.flush()

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
	}

	@Test def listStudentRelationships = transactional { tx =>

		val dept1 = Fixtures.department("pe", "Polar Exploration")
		val dept2 = Fixtures.department("mi", "Micrology")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)
		session.flush()

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		val rel1 = relationshipService.saveStudentRelationship(RelationshipType.PersonalTutor, "1000001/1", "1000003")
		val rel2 = relationshipService.saveStudentRelationship(RelationshipType.PersonalTutor, "1000002/1", "1000003")

		session.save(rel1)
		session.save(rel2)

		relationshipService.listStudentRelationshipsByDepartment(RelationshipType.PersonalTutor, dept1) should be (Seq(rel1))
		relationshipService.listStudentRelationshipsByDepartment(RelationshipType.PersonalTutor, dept2) should be (Seq(rel2))
		relationshipService.listStudentRelationshipsByDepartment(null, dept1) should be (Seq())

		relationshipService.listStudentRelationshipsWithUniversityId(RelationshipType.PersonalTutor, "1000003").toSet should be (Seq(rel1, rel2).toSet)
		relationshipService.listStudentRelationshipsWithUniversityId(RelationshipType.PersonalTutor, "1000004") should be (Seq())
		relationshipService.listStudentRelationshipsWithUniversityId(null, "1000003") should be (Seq())

		session.delete(m1)
		session.delete(m2)
		session.delete(m3)
		session.delete(m4)
		session.delete(rel1)
		session.delete(rel2)

		session.flush
	}

	@Test def listStudentsWithoutRelationship = transactional { tx =>

		val dept1 = Fixtures.department("mm", "Mumbling Modularity")
		val dept2 = Fixtures.department("dd", "Departmental Diagnostics")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)
		session.flush()

/*		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		val rel1 = relationshipService.saveStudentRelationship(RelationshipType.PersonalTutor, "1000001/1", "1000003")
		val rel2 = relationshipService.saveStudentRelationship(RelationshipType.PersonalTutor, "1000002/1", "1000003")

		session.save(rel1)
		session.save(rel2)*/

		val m5 = Fixtures.student(universityId = "1000005", userId="student", department=dept1, courseDepartment=dept1)
		val m6 = Fixtures.student(universityId = "1000006", userId="student", department=dept2, courseDepartment=dept2)

		profileService.save(m5)
		profileService.save(m6)

		relationshipService.listStudentsWithoutRelationship(RelationshipType.PersonalTutor, dept1) should be (Seq(m5))
		relationshipService.listStudentsWithoutRelationship(RelationshipType.PersonalTutor, dept2) should be (Seq(m6))
		relationshipService.listStudentsWithoutRelationship(null, dept1) should be (Seq())
	}
}
