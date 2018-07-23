package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import org.junit.Before
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}


class RemovePersonalDataAfterCourseEndedCommandTest extends TestBase with Mockito {

	class TestObject extends RemovePersonalDataAfterCourseEndedCommandHelper

	val dept1 = Fixtures.department("ms", "Motorsport")
	val stu1 = Fixtures.student(universityId = "2000001", userId = "student1", department = dept1, courseDepartment = dept1)
	val stu2 = Fixtures.student(universityId = "2000002", userId = "student2", department = dept1, courseDepartment = dept1)
	val stu3 = Fixtures.student(universityId = "2000003", userId = "student3", department = dept1, courseDepartment = dept1)

	val stu1_scd1 = Fixtures.studentCourseDetails(stu1, dept1, null, "2000001/2")
	val stu1_scd2 = Fixtures.studentCourseDetails(stu1, dept1, null, "2000001/3")

	val stu2_scd1 = Fixtures.studentCourseDetails(stu2, dept1, null, "2000002/2")
	val stu2_scd2 = Fixtures.studentCourseDetails(stu2, dept1, null, "2000002/2")

	val stu3_scd1 = Fixtures.studentCourseDetails(stu3, dept1, null, "2000003/3")

	@Before
	def setup(): Unit = {
		stu1_scd1.missingFromImportSince = null
		stu1_scd1.endDate = null
		stu1_scd2.missingFromImportSince = null
		stu1_scd2.endDate = null
		stu2_scd1.missingFromImportSince = null
		stu2_scd1.endDate = null
		stu2_scd2.missingFromImportSince = null
		stu2_scd2.endDate = null
		stu3_scd1.missingFromImportSince = null
		stu3_scd1.endDate = null
	}

	@Test
	def allHaveNullMissingAndEndDate(): Unit = {

		new TestObject().uniIDsWithEndedCourse(Seq(
			UniversityIdWithScd(stu1.universityId, Seq(stu1_scd1, stu1_scd2)),
			UniversityIdWithScd(stu2.universityId, Seq(stu2_scd1, stu2_scd2))
		)) should be(Seq.empty)
	}

	def notAllDetailsHaveEndAndMissingDate(): Unit = {
		stu1_scd1.endDate = DateTime.now.minusYears(7).toLocalDate
		stu1_scd1.missingFromImportSince = null
		new TestObject().uniIDsWithEndedCourse(Seq(
			UniversityIdWithScd(stu1.universityId, Seq(stu1_scd1))
		)) should be(Seq.empty)
		stu1_scd1.endDate = DateTime.now.minusYears(7).toLocalDate
		stu1_scd1.missingFromImportSince = null
		stu1_scd2.endDate = DateTime.now.minusYears(7).toLocalDate
		stu1_scd2.missingFromImportSince = DateTime.now.minusYears(7)
		new TestObject().uniIDsWithEndedCourse(Seq(
			UniversityIdWithScd(stu1.universityId, Seq(stu1_scd1, stu1_scd2))
		)) should be(Seq.empty)
	}

	def onlyMissingLongEnoughAgo(): Unit = {
		stu1_scd1.endDate = DateTime.now.toLocalDate
		stu1_scd1.missingFromImportSince = DateTime.now.minusYears(7)
		new TestObject().uniIDsWithEndedCourse(Seq(
			UniversityIdWithScd(stu1.universityId, Seq(stu1_scd1))
		)) should be(Seq.empty)
	}

	def studentMissingFromMemberAndHavingNoCoursedetails(): Unit = {
		stu1_scd1.endDate = DateTime.now.toLocalDate
		new TestObject().uniIDsWithEndedCourse(Seq(
			UniversityIdWithScd(stu1.universityId, Seq.empty)
		)) should be(stu1.universityId)
	}

	def missingAndEndedLongEnoughAgo(): Unit = {
		// ended long enough and missing long enough
		stu1_scd1.missingFromImportSince = DateTime.now.minusYears(2)
		stu1_scd1.endDate = DateTime.now.minusYears(7).toLocalDate
		new TestObject().uniIDsWithEndedCourse(Seq(
			UniversityIdWithScd(stu1.universityId, Seq(stu1_scd1)),
		)) should be(Seq(stu1.universityId))
	}

	def notAllCourseEndedLongEnoughAgo(): Unit = {
		// 1 course ended enough long ago, but the other is not long enough
		stu1_scd1.missingFromImportSince = DateTime.now.minusYears(2)
		stu1_scd1.endDate = DateTime.now.minusYears(2).toLocalDate
		stu1_scd2.missingFromImportSince = DateTime.now.minusYears(7)
		stu1_scd2.endDate = DateTime.now.minusYears(7).toLocalDate
		new TestObject().uniIDsWithEndedCourse(Seq(
			UniversityIdWithScd(stu1.universityId, Seq(stu1_scd1, stu1_scd2)),
		)) should be(Seq.empty)
	}

	def allCoursesEndedAndMissingLongEnoughAgo(): Unit = {
		// both courses ended and missing long enough
		stu1_scd1.missingFromImportSince = DateTime.now.minusYears(8)
		stu1_scd1.endDate = DateTime.now.minusYears(8).toLocalDate
		stu1_scd2.missingFromImportSince = DateTime.now.minusYears(7)
		stu1_scd2.endDate = DateTime.now.minusYears(7).toLocalDate
		new TestObject().uniIDsWithEndedCourse(Seq(
			UniversityIdWithScd(stu1.universityId, Seq(stu1_scd1, stu1_scd2))
		)) should be(Seq(stu1.universityId))

	}

	def allStudentsCoursesEndedAndMissingLongEnoughAgo(): Unit = {
		// both students's have all their course ended and missing long ago
		stu1_scd1.missingFromImportSince = DateTime.now.minusYears(8)
		stu1_scd1.endDate = DateTime.now.minusYears(8).toLocalDate
		stu1_scd2.missingFromImportSince = DateTime.now.minusYears(7)
		stu1_scd2.endDate = DateTime.now.minusYears(7).toLocalDate
		stu2_scd1.missingFromImportSince = DateTime.now.minusYears(8)
		stu2_scd1.endDate = DateTime.now.minusYears(8).toLocalDate
		stu2_scd2.missingFromImportSince = DateTime.now.minusYears(7)
		stu2_scd2.endDate = DateTime.now.minusYears(7).toLocalDate

		new TestObject().uniIDsWithEndedCourse(Seq(
			UniversityIdWithScd(stu1.universityId, Seq(stu1_scd1, stu1_scd2)),
			UniversityIdWithScd(stu2.universityId, Seq(stu2_scd1, stu2_scd2))
		)) should be(Seq(stu1.universityId, stu2.universityId))

		// ok with student only has 1 course
		stu1_scd1.missingFromImportSince = DateTime.now.minusYears(8)
		stu1_scd1.endDate = DateTime.now.minusYears(8).toLocalDate
		stu1_scd2.missingFromImportSince = DateTime.now.minusYears(7)
		stu1_scd2.endDate = DateTime.now.minusYears(7).toLocalDate
		stu2_scd1.missingFromImportSince = DateTime.now.minusYears(8)
		stu2_scd1.endDate = DateTime.now.minusYears(8).toLocalDate
		stu2_scd2.missingFromImportSince = DateTime.now.minusYears(7)
		stu2_scd2.endDate = DateTime.now.minusYears(7).toLocalDate
		stu3_scd1.missingFromImportSince = DateTime.now.minusYears(7)
		stu3_scd1.endDate = DateTime.now.minusYears(7).toLocalDate

		new TestObject().uniIDsWithEndedCourse(Seq(
			UniversityIdWithScd(stu1.universityId, Seq(stu1_scd1, stu1_scd2)),
			UniversityIdWithScd(stu2.universityId, Seq(stu2_scd1, stu2_scd2)),
			UniversityIdWithScd(stu3.universityId, Seq(stu3_scd1))
		)) should be(Seq(stu1.universityId, stu2.universityId, stu3.universityId))
	}
}