package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase

class StudentCourseYearDetailsTest extends TestBase with Mockito {

	@Test def comparison{
		val student = new StudentMember("0205225")

		val scd = new StudentCourseDetails(student, "0205225/1")

		val scyd_2013_1 = new StudentCourseYearDetails(scd, 1)
		scyd_2013_1.academicYear = AcademicYear(2013)

		val scyd_2013_2 = new StudentCourseYearDetails(scd, 2)
		scyd_2013_1.academicYear = AcademicYear(2013)

		val scyd_2013_3 = new StudentCourseYearDetails(scd, 3)
		scyd_2013_3.academicYear = AcademicYear(2013)

		val scyd_2012_1 = new StudentCourseYearDetails(scd, 1)
		scyd_2012_1.academicYear = AcademicYear(2012)

		val scyd_2012_2 = new StudentCourseYearDetails(scd, 2)
		scyd_2012_2.academicYear = AcademicYear(2012)

		val scyd_2012_3 = new StudentCourseYearDetails(scd, 3)
		scyd_2012_3.academicYear = AcademicYear(2012)


		// latest academic year should come back first
		scd.studentCourseYearDetails.add(scyd_2013_1)
		scd.studentCourseYearDetails.add(scyd_2012_2)

		scd.studentCourseYearDetails.asScala.max(YearAndSequenceOrdering) should be (scyd_2013_1)

		// latest sequence number should come back first
		scd.studentCourseYearDetails.clear
		scd.studentCourseYearDetails.add(scyd_2012_2)
		scd.studentCourseYearDetails.add(scyd_2012_1)

		scd.studentCourseYearDetails.asScala.max(YearAndSequenceOrdering) should be (scyd_2012_2)

		// year should win over sequence number
		scd.studentCourseYearDetails.clear
		scd.studentCourseYearDetails.add(scyd_2013_1)
		scd.studentCourseYearDetails.add(scyd_2012_2)

		scd.studentCourseYearDetails.asScala.max(YearAndSequenceOrdering) should be (scyd_2013_1)
	}
}