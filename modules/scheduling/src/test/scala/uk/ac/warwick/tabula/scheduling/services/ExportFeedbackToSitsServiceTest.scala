package uk.ac.warwick.tabula.scheduling.services

import uk.ac.warwick.tabula.{AcademicYear, Fixtures, TestBase}
import uk.ac.warwick.tabula.data.model.FeedbackForSits

class ExportFeedbackToSitsServiceTest extends TestBase {

	trait Environment {
		val year = new AcademicYear(2014)

		val module = Fixtures.module("nl901", "Foraging Forays")

		val assignment = Fixtures.assignment("Your challenge, should you choose to accept it")
		assignment.academicYear = year
		assignment.module = module

		val feedback = Fixtures.feedback("0070790")
		feedback.assignment = assignment

		val feedbackForSits = Fixtures.feedbackForSits(feedback, currentUser.apparentUser)

		val paramGetter = new ParameterGetter(feedbackForSits)

	}

	@Test
	def getQueryParams = withUser("0070790", "cusdx") {
		new Environment {
			val inspectMe = paramGetter.getQueryParams
			inspectMe.get("studentId") should be("0070790")
			inspectMe.get("academicYear") should be(year.toString)
			inspectMe.get("moduleCodeMatcher") should be("NL901%")
		}
	}

	@Test
	def getUpdateParams = withUser("0070790", "cusdx") {
		new Environment {

			val inspectMe = paramGetter.getUpdateParams(73, "A")
			inspectMe.get("studentId") should be("0070790")
			inspectMe.get("academicYear") should be(year.toString)
			inspectMe.get("moduleCodeMatcher") should be("NL901%")
			inspectMe.get("actualMark", 73)
			inspectMe.get("actualGrade", "A")
		}
	}

}
