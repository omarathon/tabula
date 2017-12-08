package uk.ac.warwick.tabula.web.controllers.attendance

import uk.ac.warwick.tabula.{AcademicYear, TestBase}

class MonthNamesTest extends TestBase {

	@Test
	def worksFor2016(): Unit = {
		MonthNames(AcademicYear(2016)) should be (Seq(
			"August 2016",
			"September 2016",
			"October 2016",
			"November 2016",
			"December 2016",
			"January 2017",
			"February 2017",
			"March 2017",
			"April 2017",
			"May 2017",
			"June 2017",
			"July 2017",
			"August 2017",
			"September 2017",
			"October 2017"
		))
	}

	@Test
	def worksFor2017(): Unit = {
		MonthNames(AcademicYear(2017)) should be (Seq(
			"August 2017",
			"September 2017",
			"October 2017",
			"November 2017",
			"December 2017",
			"January 2018",
			"February 2018",
			"March 2018",
			"April 2018",
			"May 2018",
			"June 2018",
			"July 2018",
			"August 2018",
			"September 2018"
		))
	}

	@Test
	def worksFor2018(): Unit = {
		MonthNames(AcademicYear(2018)) should be (Seq(
			"August 2018",
			"September 2018",
			"October 2018",
			"November 2018",
			"December 2018",
			"January 2019",
			"February 2019",
			"March 2019",
			"April 2019",
			"May 2019",
			"June 2019",
			"July 2019"
		))
	}

	@Test
	def worksFor2019(): Unit = {
		MonthNames(AcademicYear(2019)) should be (Seq(
			"August 2019",
			"September 2019",
			"October 2019",
			"November 2019",
			"December 2019",
			"January 2020",
			"February 2020",
			"March 2020",
			"April 2020",
			"May 2020",
			"June 2020",
			"July 2020"
		))
	}

}
