package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}


class RemovePersonalDataAfterCourseEndedCommandTest extends TestBase with Mockito {

	@Test
	def testGetUniIDsWithEndedCourse(): Unit = {

		class TestObject extends RemovePersonalDataAfterCourseEndedCommandHelper

		val dept1 = Fixtures.department("ms", "Motorsport")
		val dept2 = Fixtures.department("vr", "Vehicle Repair")
		val stu1 = Fixtures.student(universityId = "2000001", userId = "student", department = dept1, courseDepartment = dept1)
		val stu2 = Fixtures.student(universityId = "2000001", userId = "student", department = dept1, courseDepartment = dept1)

		val stu1_scd1 = Fixtures.studentCourseDetails(stu1, dept1, null, "2000001/2")
		val stu1_scd2 = Fixtures.studentCourseDetails(stu1, dept1, null, "2000001/3")

		val stu2_scd1 = Fixtures.studentCourseDetails(stu1, dept1, null, "2000001/2")
		val stu2_scd2 = Fixtures.studentCourseDetails(stu1, dept1, null, "2000001/2")


		new TestObject().uniIDsWithEndedCourse(Seq(
			Seq(stu1_scd1, stu1_scd2, stu2_scd1, stu2_scd2)
		)) should be (Seq.empty)

		stu1_scd1.endDate = DateTime.now.minusYears(7).toLocalDate

		new TestObject().uniIDsWithEndedCourse(Seq(
			Seq(stu1_scd1, stu1_scd2, stu2_scd1, stu2_scd2)
		)) should be (Seq.empty)

		stu1_scd1.missingFromImportSince = DateTime.now.minusYears(7)

		new TestObject().uniIDsWithEndedCourse(Seq(
			Seq(stu1_scd1, stu1_scd2, stu2_scd1, stu2_scd2)
		)) should be (Seq.empty)

		stu1_scd1.missingFromImportSince = DateTime.now.minusYears(2)
		stu1_scd1.endDate = DateTime.now.minusYears(7).toLocalDate

		new TestObject().uniIDsWithEndedCourse(Seq(
			Seq(stu1_scd1, stu1_scd2, stu2_scd1, stu2_scd2)
		)) should be (Seq.empty)

		stu1_scd1.missingFromImportSince = DateTime.now.minusYears(2)
		stu1_scd1.endDate = DateTime.now.minusYears(2).toLocalDate

		stu1_scd2.missingFromImportSince = DateTime.now.minusYears(7)
		stu1_scd2.endDate = DateTime.now.minusYears(7).toLocalDate

		new TestObject().uniIDsWithEndedCourse(Seq(
			Seq(stu1_scd1, stu1_scd2, stu2_scd1, stu2_scd2)
		)) should be (Seq.empty)

		stu1_scd1.missingFromImportSince = DateTime.now.minusYears(8)
		stu1_scd1.endDate = DateTime.now.minusYears(8).toLocalDate

		stu1_scd2.missingFromImportSince = DateTime.now.minusYears(7)
		stu1_scd2.endDate = DateTime.now.minusYears(7).toLocalDate

		new TestObject().uniIDsWithEndedCourse(Seq(
			Seq(stu1_scd1, stu1_scd2, stu2_scd1, stu2_scd2)
		)) should be (Seq(stu1.universityId))

	}

}