package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}


class RemovePersonalDataAfterCourseEndedCommandTest extends TestBase with Mockito {

	@Test
	def testGetUniIDsWithEndedCourse(): Unit = {

		class TestObject extends RemovePersonalDataAfterCourseEndedCommandHelper

		val dept1 = Fixtures.department("ms", "Motorsport")
		val dept2 = Fixtures.department("vr", "Vehicle Repair")
		val stu1 = Fixtures.student(universityId = "2000001", userId = "student", department = dept1, courseDepartment = dept1)

		val stu1_scd2 = Fixtures.studentCourseDetails(stu1, dept1, null, "2000001/2")
		val stu1_scd3 = Fixtures.studentCourseDetails(stu1, dept1, null, "2000001/3")


		val result = new TestObject().uniIDsWithEndedCourse(Seq(
			Seq(stu1_scd2, stu1_scd3)
		))

		result should be (Seq.empty)
	}

}