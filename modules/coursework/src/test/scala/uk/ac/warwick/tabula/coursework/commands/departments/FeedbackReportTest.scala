package uk.ac.warwick.tabula.coursework.commands.departments

import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.services.feedbackreport.FeedbackReport
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.ss.usermodel.Cell

// scalastyle:off
class FeedbackReportTest extends AppContextTestBase with ReportWorld {
	@Test
	def simpleGetSubmissionTest() {
		val userOne = new User(idFormat(1))
		userOne.setWarwickId(idFormat(1))
		val submissions = auditEventQueryMethods.submissionForStudent(assignmentOne, userOne)
		submissions.size should be (1)
	}

	@Test
	def simpleGetFeedbackTest() {
		val userOne = new User(idFormat(1))
		userOne.setWarwickId(idFormat(1))
		val publishes = auditEventQueryMethods.publishFeedbackForStudent(assignmentOne, userOne)
		publishes.size should be (1)
	}

	@Test
	def feedbackCountsTest() {
		val report = getTestFeedbackReport

		var feedbackCount = report.getFeedbackCounts(assignmentOne)
		feedbackCount should be (10,0) // 10 on time
		feedbackCount = report.getFeedbackCounts(assignmentTwo)
		feedbackCount should be (0,29) // 29 late
		feedbackCount = report.getFeedbackCounts(assignmentThree)
		feedbackCount should be (4,9) // 4 on time - 9 late
		feedbackCount = report.getFeedbackCounts(assignmentFour)
		feedbackCount should be (7,28) // 7 on time - 28 late
		feedbackCount = report.getFeedbackCounts(assignmentFive)
		feedbackCount should be (2,98) // 2 on time - 98 late
		feedbackCount = report.getFeedbackCounts(assignmentSix)
		feedbackCount should be (65,8) // 65 on time - 8 late
	}

	/**
	 * Checks that feedback published on deadline day is marked as on-time
	 *
	 **/

	@Test
	def deadlineDayTest() {

		// For close date of 28/3/2013
		// if 20 working days allowed for feedback
		// 32 days later = deadline day, which should be marked as on-time

		val report = getTestFeedbackReport
		val assignmentSeven = addAssignment("1007", "test deadline day - 1", dateTime(2013, 3, 28), 10, 0, moduleOne)
		createPublishEvent(assignmentSeven, 31, studentData(0, 10))	// on-time
		var feedbackCount = report.getFeedbackCounts(assignmentSeven)
		feedbackCount should be (10,0) // 10 on time

		val assignmentEight = addAssignment("1008", "test deadline day", dateTime(2013, 3, 28), 10, 0, moduleOne)
		createPublishEvent(assignmentEight, 32, studentData(0, 10))	// on time
		feedbackCount = report.getFeedbackCounts(assignmentEight)
		feedbackCount should be (10,0) // 10 on time

		val assignmentNine = addAssignment("1009", "test deadline day + 1", dateTime(2013, 3, 28), 10, 0, moduleOne)
		createPublishEvent(assignmentNine, 33, studentData(0, 10))	// late
		feedbackCount = report.getFeedbackCounts(assignmentNine)
		feedbackCount should be (0,10) // 10 late

	}

	@Test
	def deadlineDayTest2() {
		val report = getTestFeedbackReport
		val assignmentTen = addAssignment("1010", "test deadline day - 1", dateTime(2013, 5, 29), 10, 0, moduleOne)
		createPublishEvent(assignmentTen, 27, studentData(0, 10))	// on time
		var feedbackCount = report.getFeedbackCounts(assignmentTen)
		feedbackCount should be (10,0) // 10 on time

		val assignmentEleven = addAssignment("1010", "test deadline day", dateTime(2013, 5, 29), 10, 0, moduleOne)
		createPublishEvent(assignmentEleven, 28, studentData(0, 10))	// on time
		feedbackCount = report.getFeedbackCounts(assignmentEleven)
		feedbackCount should be (10,0) // 10 on time

		val assignmentTwelve = addAssignment("1011", "test deadline day + 1", dateTime(2013, 5, 29), 10, 0, moduleOne)
		createPublishEvent(assignmentTwelve, 29, studentData(0, 10))	// late
		feedbackCount = report.getFeedbackCounts(assignmentTwelve)
		feedbackCount should be (0, 10) // late
	}



	@Test
	def sheetTest() {
		val report = getTestFeedbackReport
		report.buildAssignmentData()
		
		val assignmentSheet = report.generateAssignmentSheet(department)
		report.populateAssignmentSheet(assignmentSheet)

		val row1 = assignmentSheet.getRow(1)
		val row1Iterator = row1.cellIterator()
		row1Iterator.next().getStringCellValue should be ("test one")
		row1Iterator.next().getStringCellValue should be ("IN101")
		row1Iterator.next().getDateCellValue should be (dateTime(2013, 3, 10).toDate)
		row1Iterator.next().getDateCellValue should be (dateTime(2013, 4, 9).toDate)
		row1Iterator.next().getStringCellValue should be ("Summative")
		row1Iterator.next().getNumericCellValue should be (10)
		row1Iterator.next().getNumericCellValue should be (10)
		row1Iterator.next().getNumericCellValue should be (0)
		row1Iterator.next().getNumericCellValue should be (2)
		row1Iterator.next().getNumericCellValue should be (10)
		row1Iterator.next().getNumericCellValue should be (10)
		row1Iterator.next().getNumericCellValue should be (1)
		row1Iterator.next().getNumericCellValue should be (0)
		row1Iterator.next().getNumericCellValue should be (0)

		val row2 = assignmentSheet.getRow(2)
		val row2Iterator = row2.cellIterator()
		row2Iterator.next().getStringCellValue should be ("test two")
		row2Iterator.next().getStringCellValue should be ("IN101")
		row2Iterator.next().getDateCellValue should be (dateTime(2013, 4, 10).toDate)
		row2Iterator.next().getDateCellValue should be (dateTime(2013, 5, 9).toDate)
		row2Iterator.next().getStringCellValue should be ("Summative")
		row2Iterator.next().getNumericCellValue should be (29)
		row2Iterator.next().getNumericCellValue should be (29)
		row2Iterator.next().getNumericCellValue should be (0)
		row2Iterator.next().getNumericCellValue should be (5)
		row2Iterator.next().getNumericCellValue should be (29)
		row2Iterator.next().getNumericCellValue should be (0)
		row2Iterator.next().getNumericCellValue should be (0)
		row2Iterator.next().getNumericCellValue should be (29)
		row2Iterator.next().getNumericCellValue should be (1)

		val row3 = assignmentSheet.getRow(3)
		val row3Iterator = row3.cellIterator()
		row3Iterator.next().getStringCellValue should be ("test three")
		row3Iterator.next().getStringCellValue should be ("IN101")
		row3Iterator.next().getDateCellValue should be (dateTime(2013, 5, 10).toDate)
		row3Iterator.next().getDateCellValue should be (dateTime(2013, 6, 10).toDate)
		row3Iterator.next().getStringCellValue should be ("Formative")
		row3Iterator.next().getNumericCellValue should be (13)
		row3Iterator.next().getNumericCellValue should be (13)
		row3Iterator.next().getNumericCellValue should be (0)
		row3Iterator.next().getNumericCellValue should be (2)
		row3Iterator.next().getNumericCellValue should be (13)
		row3Iterator.next().getNumericCellValue should be (4)
		row3Iterator.next().getNumericCellValue should be (0.307692307692307692)
		row3Iterator.next().getNumericCellValue should be (9)
		row3Iterator.next().getNumericCellValue should be (0.6923076923076923)

		val row4 = assignmentSheet.getRow(4)
		val row4Iterator = row4.cellIterator()
		row4Iterator.next().getStringCellValue should be ("test four")
		row4Iterator.next().getStringCellValue should be ("IN102")
		row4Iterator.next().getDateCellValue should be (dateTime(2013, 5, 31).toDate)
		row4Iterator.next().getDateCellValue should be (dateTime(2013, 6, 28).toDate)
		row4Iterator.next().getStringCellValue should be ("Summative")
		row4Iterator.next().getNumericCellValue should be (35)
		row4Iterator.next().getNumericCellValue should be (35)
		row4Iterator.next().getNumericCellValue should be (0)
		row4Iterator.next().getNumericCellValue should be (7)
		row4Iterator.next().getNumericCellValue should be (35)
		row4Iterator.next().getNumericCellValue should be (7)
		row4Iterator.next().getNumericCellValue should be (0.2)
		row4Iterator.next().getNumericCellValue should be (28)
		row4Iterator.next().getNumericCellValue should be (0.8)

		val row5 = assignmentSheet.getRow(6)
		val row5Iterator = row5.cellIterator()
		row5Iterator.next().getStringCellValue should be ("test five")
		row5Iterator.next().getStringCellValue should be ("IN102")
		row5Iterator.next().getDateCellValue should be (dateTime(2013, 8, 23).toDate)
		row5Iterator.next().getDateCellValue should be (dateTime(2013, 9, 23).toDate)
		row5Iterator.next().getStringCellValue should be ("Summative")
		row5Iterator.next().getNumericCellValue should be (100)
		row5Iterator.next().getNumericCellValue should be (100)
		row5Iterator.next().getNumericCellValue should be (0)
		row5Iterator.next().getNumericCellValue should be (2)
		row5Iterator.next().getNumericCellValue should be (100)
		row5Iterator.next().getNumericCellValue should be (2)
		row5Iterator.next().getNumericCellValue should be (0.02)
		row5Iterator.next().getNumericCellValue should be (98)
		row5Iterator.next().getNumericCellValue should be (0.98)

		val row6 = assignmentSheet.getRow(5)
		val row6Iterator = row6.cellIterator()
		row6Iterator.next().getStringCellValue should be ("test six")
		row6Iterator.next().getStringCellValue should be ("IN102")
		row6Iterator.next().getDateCellValue should be (dateTime(2013, 7, 1).toDate)
		row6Iterator.next().getDateCellValue should be (dateTime(2013, 7, 29).toDate)
		row6Iterator.next().getStringCellValue should be ("Summative")
		row6Iterator.next().getNumericCellValue should be (73)
		row6Iterator.next().getNumericCellValue should be (73)
		row6Iterator.next().getNumericCellValue should be (24)
		row6Iterator.next().getNumericCellValue should be (0)
		row6Iterator.next().getNumericCellValue should be (73)
		row6Iterator.next().getNumericCellValue should be (65)
		row6Iterator.next().getNumericCellValue should be (0.890410958904109589)
		row6Iterator.next().getNumericCellValue should be (8)
		row6Iterator.next().getNumericCellValue should be (0.109589041095890410)

		val moduleSheet = report.generateModuleSheet(department)
		report.populateModuleSheet(moduleSheet)

		val row7 = moduleSheet.getRow(1)
		val row7Iterator = row7.cellIterator()
		row7Iterator.next().getStringCellValue should be ("Module One")
		row7Iterator.next().getStringCellValue should be ("IN101")
		row7Iterator.next().getNumericCellValue should be (3)
		row7Iterator.next().getNumericCellValue should be (52)
		row7Iterator.next().getNumericCellValue should be (52)
		row7Iterator.next().getNumericCellValue should be (0)
		row7Iterator.next().getNumericCellValue should be (9)
		row7Iterator.next().getNumericCellValue should be (14)
		row7Iterator.next().getNumericCellValue should be (0.269230769230769230)
		row7Iterator.next().getNumericCellValue should be (38)
		row7Iterator.next().getNumericCellValue should be (0.730769230769230769)

		val row8 = moduleSheet.getRow(2)
		val row8Iterator = row8.cellIterator()
		row8Iterator.next().getStringCellValue should be ("Module Two")
		row8Iterator.next().getStringCellValue should be ("IN102")
		row8Iterator.next().getNumericCellValue should be (3)
		row8Iterator.next().getNumericCellValue should be (208)
		row8Iterator.next().getNumericCellValue should be (208)
		row8Iterator.next().getNumericCellValue should be (24)
		row8Iterator.next().getNumericCellValue should be (9)
		row8Iterator.next().getNumericCellValue should be (74)
		row8Iterator.next().getNumericCellValue should be (0.3557692307692307692)
		row8Iterator.next().getNumericCellValue should be (134)
		row8Iterator.next().getNumericCellValue should be (0.6442307692307692307)
	}

	def getTestFeedbackReport = {
		val report = new FeedbackReport(department, dateTime(2013, 3, 1), dateTime(2013, 9, 30))
		report.assignmentMembershipService = assignmentMembershipService
		report.auditEventQueryMethods = auditEventQueryMethods
		report.submissionService = submissionService
		report
	}

}
