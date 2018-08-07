package uk.ac.warwick.tabula.data

import org.joda.time.DateTime
import org.junit.Before
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, PersistenceTestBase}

class StudentCourseYearDetailsDaoTest extends PersistenceTestBase {

	val scydDao = new StudentCourseYearDetailsDaoImpl
	val scdDao = new StudentCourseDetailsDaoImpl
	val memDao = new AutowiringMemberDaoImpl

	@Before
	def setup() {
		scydDao.sessionFactory = sessionFactory
		scdDao.sessionFactory = sessionFactory
		memDao.sessionFactory = sessionFactory
	}

	@Test def deleteScdByIdShouldNotDeleteAssociatedScyd(): Unit = transactional{ tx =>
		val stuMem = new StudentMember("0123456")
		stuMem.userId = "abcde"
		memDao.saveOrUpdate(stuMem)
		val scd = new StudentCourseDetails(stuMem, "0123456/1")
		scd.sprCode = "0123456/2"
		scdDao.saveOrUpdate(scd)

		val nonexistantYearDetails = scydDao.getBySceKey(scd, 1)
		nonexistantYearDetails should be (None)

		val scyd = new StudentCourseYearDetails(scd, 1,AcademicYear(2013))

		scd.addStudentCourseYearDetails(scyd)

		scydDao.saveOrUpdate(scyd)
		scdDao.saveOrUpdate(scd)

		scd.freshStudentCourseYearDetails.size should be (1)
		var retrievedScyd = scydDao.getBySceKey(scd, 1).get
		retrievedScyd.isInstanceOf[StudentCourseYearDetails] should be (true)
		retrievedScyd.studentCourseDetails.scjCode should be ("0123456/1")
		retrievedScyd.studentCourseDetails.sprCode should be ("0123456/2")
		retrievedScyd.academicYear should be (AcademicYear(2013))
		scdDao.deleteByIds(Seq(scd.scjCode))
		retrievedScyd = scydDao.getBySceKey(scd, 1).get
		retrievedScyd.isInstanceOf[StudentCourseYearDetails] should be (true)
		retrievedScyd.studentCourseDetails.scjCode should be ("0123456/1")
		retrievedScyd.studentCourseDetails.sprCode should be ("0123456/2")
		retrievedScyd.academicYear should be (AcademicYear(2013))
	}

	@Test def deleteScdShouldDeleteAssociatedScyd(): Unit = transactional { tx =>

		val stuMem = new StudentMember("0123456")
		stuMem.userId = "abcde"
		memDao.saveOrUpdate(stuMem)
		val scd = new StudentCourseDetails(stuMem, "0123456/1")
		scd.sprCode = "0123456/2"
		scdDao.saveOrUpdate(scd)

		val nonexistantYearDetails = scydDao.getBySceKey(scd, 1)
		nonexistantYearDetails should be (None)

		val scyd = new StudentCourseYearDetails(scd, 1,AcademicYear(2013))

		scd.addStudentCourseYearDetails(scyd)

		scydDao.saveOrUpdate(scyd)
		scdDao.saveOrUpdate(scd)

		scd.freshStudentCourseYearDetails.size should be (1)
		val retrievedScyd = scydDao.getBySceKey(scd, 1).get
		retrievedScyd.isInstanceOf[StudentCourseYearDetails] should be (true)
		retrievedScyd.studentCourseDetails.scjCode should be ("0123456/1")
		retrievedScyd.studentCourseDetails.sprCode should be ("0123456/2")
		retrievedScyd.academicYear should be (AcademicYear(2013))
		scdDao.delete(scd)
		scydDao.getBySceKey(scd, 1) should be (Option.empty)
	}

	@Test def testGetBySceKey() {
		transactional { tx =>
			val stuMem = new StudentMember("0123456")
			stuMem.userId = "abcde"
			memDao.saveOrUpdate(stuMem)
			val scd = new StudentCourseDetails(stuMem, "0123456/1")
			scd.sprCode = "0123456/2"
			scdDao.saveOrUpdate(scd)

			val nonexistantYearDetails = scydDao.getBySceKey(scd, 1)
			nonexistantYearDetails should be (None)

			val scyd = new StudentCourseYearDetails(scd, 1,AcademicYear(2013))

			scd.addStudentCourseYearDetails(scyd)

			scydDao.saveOrUpdate(scyd)
			scdDao.saveOrUpdate(scd)

			scd.freshStudentCourseYearDetails.size should be (1)

			val retrievedScyd = scydDao.getBySceKey(scd, 1).get
			retrievedScyd.isInstanceOf[StudentCourseYearDetails] should be (true)
			retrievedScyd.studentCourseDetails.scjCode should be ("0123456/1")
			retrievedScyd.studentCourseDetails.sprCode should be ("0123456/2")
			retrievedScyd.academicYear should be (AcademicYear(2013))
		}
	}

	@Test def testGetFreshKeys() {
		transactional { tx =>
			val dept1 = Fixtures.department("hm", "History of Music")
			val dept2 = Fixtures.department("ar", "Architecture")

			session.saveOrUpdate(dept1)
			session.saveOrUpdate(dept2)

			val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1)
			val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2)
			val stu3 = Fixtures.student(universityId = "1000003", userId="student", department=dept2, courseDepartment=dept2)
			val stu4 = Fixtures.student(universityId = "1000004", userId="student", department=dept2, courseDepartment=dept2)

			session.saveOrUpdate(stu1)
			session.saveOrUpdate(stu2)
			session.saveOrUpdate(stu3)
			session.saveOrUpdate(stu4)

			scydDao.getFreshKeys.size should be (4)

			scydDao.getFreshKeys.head.scjCode.length() should be (9)
			val scjCodes = scydDao.getFreshKeys map { key => key.getScjCode}
			scjCodes.contains("1000001/1") should be (true)
			scjCodes.contains("1000002/1") should be (true)
			scjCodes.contains("1000003/1") should be (true)
			scjCodes.contains("1000004/1") should be (true)

			scydDao.getFreshKeys.head.sceSequenceNumber should be (1)

			val scyd = stu2.mostSignificantCourse.freshStudentCourseYearDetails.head

			scyd.missingFromImportSince = DateTime.now
			session.saveOrUpdate(scyd)

			scydDao.getFreshKeys.size should be (3)
		}
	}

	@Test
	def testStampMissingFromImport(): Unit = transactional { tx =>
		val dept1 = Fixtures.department("hm", "History of Music")
		val dept2 = Fixtures.department("ar", "Architecture")

		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1)
		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2)
		val stu3 = Fixtures.student(universityId = "1000003", userId="student", department=dept2, courseDepartment=dept2)
		val stu4 = Fixtures.student(universityId = "1000004", userId="student", department=dept2, courseDepartment=dept2)

		memDao.saveOrUpdate(stu1)
		memDao.saveOrUpdate(stu2)
		memDao.saveOrUpdate(stu3)
		memDao.saveOrUpdate(stu4)
		session.flush()
		session.clear()

		val key1 = new StudentCourseYearKey(stu1.mostSignificantCourse.scjCode, 1)
		val key3 = new StudentCourseYearKey(stu3.mostSignificantCourse.scjCode, 1)

		val scyd1 = scydDao.getBySceKey(stu1.mostSignificantCourse, 1).get
		val scyd2 = scydDao.getBySceKey(stu2.mostSignificantCourse, 1).get
		val scyd3 = scydDao.getBySceKey(stu3.mostSignificantCourse, 1).get
		val scyd4 = scydDao.getBySceKey(stu4.mostSignificantCourse, 1).get

		scydDao.getBySceKey(stu1.mostSignificantCourse, 1) should be (Some(scyd1))
		scydDao.getBySceKey(stu2.mostSignificantCourse, 1) should be (Some(scyd2))
		scydDao.getBySceKey(stu3.mostSignificantCourse, 1) should be (Some(scyd3))
		scydDao.getBySceKey(stu4.mostSignificantCourse, 1) should be (Some(scyd4))

		scyd1.missingFromImportSince should be (null)
		scyd2.missingFromImportSince should be (null)
		scyd3.missingFromImportSince should be (null)
		scyd4.missingFromImportSince should be (null)

		val seenSceKeys = Seq(key1, key3)

		val seenIds = scydDao.convertKeysToIds(seenSceKeys)

		// these 3 lines are in fact ImportRowTracker.newStaleScydIdeas but that's in scheduling so can't be accessed:
		val scydIdsSeen = scydDao.convertKeysToIds(seenSceKeys)
		val allFreshIds = scydDao.getFreshIds.toSet
		val newStaleScydIds = (allFreshIds -- scydIdsSeen).toSeq

		val importStart = DateTime.now
		scydDao.stampMissingFromImport(newStaleScydIds, importStart)

		for (id <- seenIds) logger.warn("seen ID: " + id)

		session.flush()
		session.clear()

		scydDao.getBySceKey(stu1.mostSignificantCourse, 1) should be (Some(scyd1))
		scydDao.getBySceKey(stu2.mostSignificantCourse, 1) should be (None)
		scydDao.getBySceKey(stu3.mostSignificantCourse, 1) should be (Some(scyd3))
		scydDao.getBySceKey(stu4.mostSignificantCourse, 1) should be (None)

	}

	@Test
	def testUnstampPresentInImport(): Unit = transactional { tx =>
		val dept1 = Fixtures.department("hm", "History of Music")
		val dept2 = Fixtures.department("ar", "Architecture")

		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1)
		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2)
		val stu3 = Fixtures.student(universityId = "1000003", userId="student", department=dept2, courseDepartment=dept2)
		val stu4 = Fixtures.student(universityId = "1000004", userId="student", department=dept2, courseDepartment=dept2)

		stu2.mostSignificantCourse.latestStudentCourseYearDetails.missingFromImportSince = DateTime.now
		stu3.mostSignificantCourse.latestStudentCourseYearDetails.missingFromImportSince = DateTime.now
		stu4.mostSignificantCourse.latestStudentCourseYearDetails.missingFromImportSince = DateTime.now

		memDao.saveOrUpdate(stu1)
		memDao.saveOrUpdate(stu2)
		memDao.saveOrUpdate(stu3)
		memDao.saveOrUpdate(stu4)
		session.flush()
		session.clear()

		scydDao.getBySceKey(stu1.mostSignificantCourse, 1).get.missingFromImportSince should be (null)
		scydDao.getBySceKey(stu2.mostSignificantCourse, 1) should be (None)
		scydDao.getBySceKey(stu3.mostSignificantCourse, 1) should be (None)
		scydDao.getBySceKey(stu4.mostSignificantCourse, 1) should be (None)

		val key2 = new StudentCourseYearKey(stu2.mostSignificantCourse.scjCode, 1)
		val key3 = new StudentCourseYearKey(stu3.mostSignificantCourse.scjCode, 1)
		val notStaleSceKeys = Seq(key2, key3)
		val notStaleIds = scydDao.convertKeysToIds(notStaleSceKeys)
		scydDao.unstampPresentInImport(notStaleIds)

		session.flush()
		session.clear()

		scydDao.getBySceKey(stu1.mostSignificantCourse, 1).get.missingFromImportSince should be (null)
		scydDao.getBySceKey(stu2.mostSignificantCourse, 1).get.missingFromImportSince should be (null)
		scydDao.getBySceKey(stu3.mostSignificantCourse, 1).get.missingFromImportSince should be (null)
		scydDao.getBySceKey(stu4.mostSignificantCourse, 1) should be (None)

	}

	@Test def getOrphanedScyds: Unit = transactional { tx =>
		val dept1 = Fixtures.department("hm", "History of Music")
		val dept2 = Fixtures.department("ar", "Architecture")

		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1)
		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2)
		val stu3 = Fixtures.student(universityId = "1000003", userId="student", department=dept2, courseDepartment=dept2)
		val stu4 = Fixtures.student(universityId = "1000004", userId="student", department=dept2, courseDepartment=dept2)

		session.saveOrUpdate(stu1)
		session.saveOrUpdate(stu2)
		session.saveOrUpdate(stu3)
		session.saveOrUpdate(stu4)

		scydDao.getFreshKeys.size should be (4)

		scydDao.getFreshKeys.head.scjCode.length() should be (9)
		val scjCodes = scydDao.getFreshKeys map { key => key.getScjCode}
		scjCodes.contains("1000001/1") should be (true)
		scjCodes.contains("1000002/1") should be (true)
		scjCodes.contains("1000003/1") should be (true)
		scjCodes.contains("1000004/1") should be (true)

		scydDao.getFreshKeys.head.sceSequenceNumber should be (1)

		val scyd: StudentCourseYearDetails = stu2.mostSignificantCourse.freshStudentCourseYearDetails.head

		scyd.missingFromImportSince = DateTime.now
		scyd.academicYear = AcademicYear.forDate(DateTime.now().minusYears(8))
		session.saveOrUpdate(scyd)
		scdDao.deleteByIds(Seq(scyd.studentCourseDetails.scjCode))

		scydDao.getOrphaned.head should be (scyd.id)
		scydDao.getFreshIds.size should be (3)
	}

	@Test def deleteByIds: Unit = transactional { tx =>
		val dept1 = Fixtures.department("hm", "History of Music")
		val dept2 = Fixtures.department("ar", "Architecture")

		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1)
		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2)
		val stu3 = Fixtures.student(universityId = "1000003", userId="student", department=dept2, courseDepartment=dept2)
		val stu4 = Fixtures.student(universityId = "1000004", userId="student", department=dept2, courseDepartment=dept2)

		val students = Seq(stu1, stu2, stu3, stu4)

		students.foreach(session.saveOrUpdate)

		scydDao.getFreshKeys.size should be (4)

		scydDao.getFreshKeys.head.scjCode.length() should be (9)
		val scjCodes = scydDao.getFreshKeys map { key => key.getScjCode}
		scjCodes.contains("1000001/1") should be (true)
		scjCodes.contains("1000002/1") should be (true)
		scjCodes.contains("1000003/1") should be (true)
		scjCodes.contains("1000004/1") should be (true)

		scydDao.getFreshKeys.head.sceSequenceNumber should be (1)

		val scyd: StudentCourseYearDetails = stu2.mostSignificantCourse.freshStudentCourseYearDetails.head

		session.saveOrUpdate(scyd)
		session.flush()
		scydDao.deleteByIds(scydDao.getFreshIds)
		scydDao.getFreshIds.size should be (0)
	}

	@Test
	def findByCourseAndRoutes(): Unit = transactional { tx =>
		val dept1 = Fixtures.department("hm", "History of Music")
		val dept2 = Fixtures.department("ar", "Architecture")

		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)

		val course1 = Fixtures.course("U100-ABCD")
		val course2 = Fixtures.course("U100-EFGH")
		val route1 = Fixtures.route("a100")
		val route2 = Fixtures.route("a101")
		val route3 = Fixtures.route("e100")

		session.saveOrUpdate(course1)
		session.saveOrUpdate(course2)
		session.saveOrUpdate(route1)
		session.saveOrUpdate(route2)
		session.saveOrUpdate(route3)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1)
		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2)
		val stu3 = Fixtures.student(universityId = "1000003", userId="student", department=dept2, courseDepartment=dept2)
		val stu4 = Fixtures.student(universityId = "1000004", userId="student", department=dept2, courseDepartment=dept2)

		memDao.saveOrUpdate(stu1)
		memDao.saveOrUpdate(stu2)
		memDao.saveOrUpdate(stu3)
		memDao.saveOrUpdate(stu4)

		val key1 = new StudentCourseYearKey(stu1.mostSignificantCourse.scjCode, 1)
		val key3 = new StudentCourseYearKey(stu3.mostSignificantCourse.scjCode, 1)

		val scyd1 = scydDao.getBySceKey(stu1.mostSignificantCourse, 1).get
		scyd1.studentCourseDetails.course = course1
		scyd1.studentCourseDetails.currentRoute = route1
		scyd1.enrolledOrCompleted = true
		scyd1.studentCourseDetails.statusOnRoute = Fixtures.sitsStatus()

		val uag = Fixtures.assessmentGroup(AcademicYear.now(), "", "", "A01")
		val uagm = new UpstreamAssessmentGroupMember(uag, "1000001")
		uagm.resitActualMark = Some(41)
		uagm.resitActualGrade = Some("3")
		uag.members.add(uagm)

		session.saveOrUpdate(uag)
		session.saveOrUpdate(uagm)

		val scyd2 = scydDao.getBySceKey(stu2.mostSignificantCourse, 1).get
		scyd2.studentCourseDetails.course = course1
		scyd2.studentCourseDetails.currentRoute = route2
		scyd2.enrolledOrCompleted = true
		scyd2.studentCourseDetails.statusOnRoute = Fixtures.sitsStatus()
		val scyd3 = scydDao.getBySceKey(stu3.mostSignificantCourse, 1).get
		scyd3.studentCourseDetails.course = course2
		scyd3.studentCourseDetails.currentRoute = route2
		scyd3.enrolledOrCompleted = true
		scyd3.studentCourseDetails.statusOnRoute = Fixtures.sitsStatus()
		val scyd4 = scydDao.getBySceKey(stu4.mostSignificantCourse, 1).get
		scyd4.studentCourseDetails.course = course2
		scyd4.studentCourseDetails.currentRoute = route2
		scyd4.enrolledOrCompleted = true
		scyd4.studentCourseDetails.statusOnRoute = Fixtures.sitsStatus("T")

		session.saveOrUpdate(scyd1)
		session.saveOrUpdate(scyd2)
		session.saveOrUpdate(scyd3)
		session.saveOrUpdate(scyd4)
		session.saveOrUpdate(scyd1.studentCourseDetails)
		session.saveOrUpdate(scyd2.studentCourseDetails)
		session.saveOrUpdate(scyd3.studentCourseDetails)
		session.saveOrUpdate(scyd4.studentCourseDetails)


		session.flush()
		session.clear()

		scydDao.findByCourseRoutesYear(
			AcademicYear.now(),
			Seq(course1),
			Seq(),
			1,
			false,
			false
		).length should be(2)

		scydDao.findByCourseRoutesYear(
			AcademicYear.now(),
			Seq(course2),
			Seq(),
			1,
			false,
			false
		).length should be(1)

		scydDao.findByCourseRoutesLevel(
			AcademicYear.now(),
			Seq(course2),
			Seq(),
			"1",
			true,
			false
		).length should be(2)

		scydDao.findByCourseRoutesLevel(
			AcademicYear.now(),
			Seq(course1, course2),
			Seq(route1),
			"1",
			true,
			false
		).length should be(1)

		scydDao.findByCourseRoutesYear(
			AcademicYear.now(),
			Seq(course1, course2),
			Seq(),
			1,
			true,
			true
		).length should be(1)

	}

}
