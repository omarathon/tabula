package uk.ac.warwick.tabula.services

import scala.collection.JavaConverters.asScalaBufferConverter

import org.joda.time.{DateTime, DateTimeConstants, DateTimeUtils}
import org.junit.{After, Before}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

import uk.ac.warwick.tabula.{AppContextTestBase, Fixtures}
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.SitsStatusDaoImpl
import uk.ac.warwick.tabula.data.model.{Member, StudentRelationshipType}

// scalastyle:off magic.number
class RelationshipServiceTest extends AppContextTestBase with Mockito {

	@Autowired var relationshipService: RelationshipServiceImpl = _
	@Autowired var profileService: ProfileServiceImpl = _
	val sitsStatusDao = new SitsStatusDaoImpl
	val sprFullyEnrolledStatus = Fixtures.sitsStatus("F", "Fully Enrolled", "Fully Enrolled for this Session")
	var sprWithdrawnStatus = Fixtures.sitsStatus("PX", "PWD X", "Permanently Withdrawn with Nuance")

	@Transactional
	@Test def findingRelationships = withFakeTime(dateTime(2000, 6)) {
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipService.saveOrUpdate(relationshipType)

		relationshipService.findCurrentRelationships(relationshipType, "1250148/1") should be ('empty)
		relationshipService.saveStudentRelationships(relationshipType, "1250148/1", Seq("1234567"))

		val opt = relationshipService.findCurrentRelationships(relationshipType, "1250148/1").headOption
		val currentRelationship = opt.getOrElse(fail("Failed to get current relationship"))
		currentRelationship.agent should be ("1234567")

		// now we've stored a relationship.  Try storing the identical one again:
		relationshipService.saveStudentRelationships(relationshipType, "1250148/1", Seq("1234567"))

		relationshipService.findCurrentRelationships(relationshipType, "1250148/1").headOption.getOrElse(fail("Failed to get current relationship after re-storing")).agent should be ("1234567")
		relationshipService.getRelationships(relationshipType, "1250148/1").size should be (1)

		// now store a new personal tutor for the same student. We should now have TWO personal tutors (as of TAB-416)
		DateTimeUtils.setCurrentMillisFixed(new DateTime().plusMillis(30).getMillis())
		relationshipService.saveStudentRelationships(relationshipType, "1250148/1", Seq("7654321"))

		val rels = relationshipService.getRelationships(relationshipType, "1250148/1")

		DateTimeUtils.setCurrentMillisFixed(new DateTime().plusMillis(30).getMillis())
		val currentRelationshipsUpdated = relationshipService.findCurrentRelationships(relationshipType, "1250148/1")
		currentRelationshipsUpdated.size should be (2)

		currentRelationshipsUpdated.find(_.agent == "7654321") should be ('defined)

		relationshipService.getRelationships(relationshipType, "1250148/1").size should be (2)

		val stu = Fixtures.student(universityId = "1250148", userId="student", sprStatus=sprFullyEnrolledStatus)
		stu.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		profileService.save(stu)

		relationshipService.listStudentRelationshipsWithUniversityId(relationshipType, "1234567").size should be (1)
		relationshipService.listStudentRelationshipsWithUniversityId(relationshipType, "7654321").size should be (1)
		relationshipService.listStudentRelationshipsWithUniversityId(relationshipType, "7654321").head.targetSprCode should be ("1250148/1")
	}

	@Before def setup: Unit = transactional { tx =>
		sitsStatusDao.sessionFactory = sessionFactory
		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

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

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, sprStatus=sprFullyEnrolledStatus)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, sprStatus=sprFullyEnrolledStatus)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipService.saveOrUpdate(relationshipType)

		val rel1 = relationshipService.saveStudentRelationships(relationshipType, "0000001/1", Seq("0000003")).head
		val rel2 = relationshipService.saveStudentRelationships(relationshipType, "0000002/1", Seq("0000003")).head

		session.save(rel1)
		session.save(rel2)

		relationshipService.findCurrentRelationships(relationshipType, "0000001/1") should be (Seq(rel1))
		relationshipService.findCurrentRelationships(relationshipType, "0000002/1") should be (Seq(rel2))
		relationshipService.findCurrentRelationships(relationshipType, "0000003/1") should be (Nil)
		relationshipService.findCurrentRelationships(null, "0000001/1") should be (Nil)

		relationshipService.getRelationships(relationshipType, "0000001/1") should be (Seq(rel1))
		relationshipService.getRelationships(relationshipType, "0000002/1") should be (Seq(rel2))
		relationshipService.getRelationships(relationshipType, "0000003/1") should be (Seq())
		relationshipService.getRelationships(null, "0000001/1") should be (Seq())
	}

	@Test def saveReplaceStudentRelationships = transactional { tx =>

		val dept1 = Fixtures.department("el", "Equatorial Life")
		val dept2 = Fixtures.department("nr", "Northern Regions")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)
		session.flush()

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, sprStatus=sprFullyEnrolledStatus)
		stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val staff1 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		staff1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val staff2 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		staff2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		profileService.save(stu1)
		profileService.save(staff1)
		profileService.save(staff2)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipService.saveOrUpdate(relationshipType)

		// create and save one personal tutor relationship
		val relStu1Staff3 = relationshipService.saveStudentRelationships(relationshipType, "1000001/1", Seq("1000003")).head
		session.save(relStu1Staff3)
		relationshipService.getRelationships(relationshipType, "1000001/1") should be (Seq(relStu1Staff3))
		relationshipService.findCurrentRelationships(relationshipType, "1000001/1") should be (Seq(relStu1Staff3))

		// now try saving the same again - shouldn't make any difference
		val relStu1Staff3_2ndAttempt = relationshipService.saveStudentRelationships(relationshipType, "1000001/1", Seq("1000003")).head
		session.save(relStu1Staff3_2ndAttempt)
		relationshipService.getRelationships(relationshipType, "1000001/1") should be (Seq(relStu1Staff3))
		relationshipService.findCurrentRelationships(relationshipType, "1000001/1") should be (Seq(relStu1Staff3))

		// now replace the existing personal tutor with a new one
		relStu1Staff3.endDate should be (null)
		val relStu1Staff4 = relationshipService.replaceStudentRelationships(relationshipType, "1000001/1", Seq("1000004")).head
		session.save(relStu1Staff4)
		relationshipService.getRelationships(relationshipType, "1000001/1") should be (Seq(relStu1Staff3, relStu1Staff4))
		relationshipService.findCurrentRelationships(relationshipType, "1000001/1") should be (Seq(relStu1Staff4))
		relStu1Staff3.endDate should not be (null)

		// now save the original personal tutor again:
		val relStu1Staff3_3rdAttempt = relationshipService.saveStudentRelationships(relationshipType, "1000001/1", Seq("1000003")).head
		session.save(relStu1Staff3_3rdAttempt)
		relationshipService.getRelationships(relationshipType, "1000001/1") should be (Seq(relStu1Staff3, relStu1Staff4, relStu1Staff3_3rdAttempt))
		relationshipService.findCurrentRelationships(relationshipType, "1000001/1") should be (Seq(relStu1Staff4, relStu1Staff3_3rdAttempt))
		relStu1Staff4.endDate should be (null)
		relStu1Staff3_3rdAttempt.endDate should be (null)

		// If there is an old relationship but no current relationship, the relationship should be recreated.
		// End the current relationships:
		relStu1Staff3_3rdAttempt.endDate = DateTime.now
		relStu1Staff4.endDate = DateTime.now
		session.flush
		relationshipService.findCurrentRelationships(relationshipType, "1000001/1") should be (Seq())

		// then recreate one of them using a call to replaceStudentRelationship:
		val relStu1Staff4_2ndAttempt = relationshipService.replaceStudentRelationships(relationshipType, "1000001/1", Seq("1000004")).head
		session.save(relStu1Staff4_2ndAttempt)
		session.flush
		relationshipService.findCurrentRelationships(relationshipType, "1000001/1") should be (Seq(relStu1Staff4_2ndAttempt))
	}


	@Test def listStudentRelationships = transactional { tx =>

		val dept1 = Fixtures.department("pe", "Polar Exploration")
		val dept2 = Fixtures.department("mi", "Micrology")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprFullyEnrolledStatus)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipService.saveOrUpdate(relationshipType)

		val rel1 = relationshipService.saveStudentRelationships(relationshipType, "1000001/1", Seq("1000003")).head
		val rel2 = relationshipService.saveStudentRelationships(relationshipType, "1000002/1", Seq("1000003")).head

		session.save(rel1)
		session.save(rel2)

		relationshipService.listStudentRelationshipsByDepartment(relationshipType, dept1) should be (Seq(rel1))
		relationshipService.listStudentRelationshipsByDepartment(relationshipType, dept2) should be (Seq(rel2))

		relationshipService.listStudentRelationshipsWithUniversityId(relationshipType, "1000003").toSet should be (Seq(rel1, rel2).toSet)
		relationshipService.listStudentRelationshipsWithUniversityId(relationshipType, "1000004") should be (Seq())
	}

	@Test def listStudentsWithoutRelationship = transactional { tx =>

		val dept1 = Fixtures.department("mm", "Mumbling Modularity")
		val dept2 = Fixtures.department("dd", "Departmental Diagnostics")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val m5 = Fixtures.student(universityId = "1000005", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		val m6 = Fixtures.student(universityId = "1000006", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprFullyEnrolledStatus)

		profileService.save(m5)
		profileService.save(m6)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipService.saveOrUpdate(relationshipType)

		relationshipService.listStudentsWithoutRelationship(relationshipType, dept1) should be (Seq(m5))
		relationshipService.listStudentsWithoutRelationship(relationshipType, dept2) should be (Seq(m6))
		relationshipService.listStudentsWithoutRelationship(null, dept1) should be (Seq())
	}

	@Test def testRelationshipNotPermanentlyWithdrawn = transactional { tx =>
		val dept1 = Fixtures.department("pe", "Polar Exploration")
		val dept2 = Fixtures.department("mi", "Micrology")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)
		sitsStatusDao.saveOrUpdate(sprWithdrawnStatus)

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprWithdrawnStatus)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipService.saveOrUpdate(relationshipType)

		val rel1 = relationshipService.saveStudentRelationships(relationshipType, "1000001/1", Seq("1000003")).head
		val rel2 = relationshipService.saveStudentRelationships(relationshipType, "1000002/1", Seq("1000003")).head

		session.save(rel1)
		session.save(rel2)

		relationshipService.relationshipNotPermanentlyWithdrawn(rel1) should be (true)
		relationshipService.relationshipNotPermanentlyWithdrawn(rel2) should be (false)

	}
}
