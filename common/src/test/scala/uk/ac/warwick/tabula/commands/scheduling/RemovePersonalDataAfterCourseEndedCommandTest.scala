package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}


class RemovePersonalDataAfterCourseEndedCommandTest extends TestBase with Mockito {

	@Test
	def testGetUniIDsWithEndedCourse(): Unit = {

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

		// no ended data, no missing since data
		new TestObject().uniIDsWithEndedCourse(Seq(
			Seq(stu1_scd1, stu1_scd2),
			Seq(stu2_scd1, stu2_scd2)
		)) should be(Seq.empty)

		// only ended long enough
		stu1_scd1.endDate = DateTime.now.minusYears(7).toLocalDate
		stu1_scd1.missingFromImportSince = null
		new TestObject().uniIDsWithEndedCourse(Seq(
			Seq(stu1_scd1, stu1_scd2),
			Seq(stu2_scd1, stu2_scd2)
		)) should be(Seq.empty)

		// only missing long enough
		stu1_scd1.endDate = null
		stu1_scd1.missingFromImportSince = DateTime.now.minusYears(7)
		new TestObject().uniIDsWithEndedCourse(Seq(
			Seq(stu1_scd1, stu1_scd2),
			Seq(stu2_scd1, stu2_scd2)
		)) should be(Seq.empty)

		// ended long enough but not missing long enough
		stu1_scd1.missingFromImportSince = DateTime.now.minusYears(2)
		stu1_scd1.endDate = DateTime.now.minusYears(7).toLocalDate
		new TestObject().uniIDsWithEndedCourse(Seq(
			Seq(stu1_scd1, stu1_scd2),
			Seq(stu2_scd1, stu2_scd2)
		)) should be(Seq.empty)

		// 1 course ended enough long ago, but the other is not long enough
		stu1_scd1.missingFromImportSince = DateTime.now.minusYears(2)
		stu1_scd1.endDate = DateTime.now.minusYears(2).toLocalDate
		stu1_scd2.missingFromImportSince = DateTime.now.minusYears(7)
		stu1_scd2.endDate = DateTime.now.minusYears(7).toLocalDate
		new TestObject().uniIDsWithEndedCourse(Seq(
			Seq(stu1_scd1, stu1_scd2),
			Seq(stu2_scd1, stu2_scd2)
		)) should be(Seq.empty)

		// both courses ended and missing long enough
		stu1_scd1.missingFromImportSince = DateTime.now.minusYears(8)
		stu1_scd1.endDate = DateTime.now.minusYears(8).toLocalDate
		stu1_scd2.missingFromImportSince = DateTime.now.minusYears(7)
		stu1_scd2.endDate = DateTime.now.minusYears(7).toLocalDate
		new TestObject().uniIDsWithEndedCourse(Seq(
			Seq(stu1_scd1, stu1_scd2, stu2_scd1, stu2_scd2)
		)) should be(Seq(stu1.universityId))


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
			Seq(stu1_scd1, stu1_scd2),
			Seq(stu2_scd1, stu2_scd2)
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
			Seq(stu1_scd1, stu1_scd2),
			Seq(stu2_scd1, stu2_scd2),
			Seq(stu3_scd1)
		)) should be(Seq(stu1.universityId, stu2.universityId, stu3.universityId))


	}

}