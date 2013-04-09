package uk.ac.warwick.tabula.coursework.commands.departments

import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.userlookup.User

class FeedbackReportCommandTest extends AppContextTestBase with ReportWorld {
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
		val command = getTestCommand()
		command.assignmentMembershipService = assignmentMembershipService
		command.auditEventQueryMethods = auditEventQueryMethods

		var feedbackCount = command.getFeedbackCounts(assignmentOne)
		feedbackCount should be (10,0) // 10 on time
		feedbackCount = command.getFeedbackCounts(assignmentTwo)
		feedbackCount should be (0,29) // 29 late
		feedbackCount = command.getFeedbackCounts(assignmentThree)
		feedbackCount should be (4,9) // 4 on time - 9 late
		feedbackCount = command.getFeedbackCounts(assignmentFour)
		feedbackCount should be (7,28) // 7 on time - 28 late
		feedbackCount = command.getFeedbackCounts(assignmentFive)
		feedbackCount should be (2,98) // 2 on time - 98 late
		feedbackCount = command.getFeedbackCounts(assignmentSix)
		feedbackCount should be (65,8) // 65 on time - 8 late
	}

	@Test
	def sheetTest() {
		val command = getTestCommand()
		command.assignmentMembershipService = assignmentMembershipService
		command.auditEventQueryMethods = auditEventQueryMethods
		command.buildAssignmentData()
		
		val assignmentSheet = command.generateAssignmentSheet(department)
		command.populateAssignmentSheet(assignmentSheet)
		
		val row1 = assignmentSheet.getRow(1)
		row1.getCell(0).getStringCellValue should be ("test one")
		row1.getCell(1).getStringCellValue should be ("IN101")
		row1.getCell(2).getDateCellValue should be (dateTime(2013, 3, 10).toDate)
		row1.getCell(3).getNumericCellValue should be (10)
		row1.getCell(4).getNumericCellValue should be (10)
		row1.getCell(5).getNumericCellValue should be (0)
		row1.getCell(6).getNumericCellValue should be (2)
		row1.getCell(7).getNumericCellValue should be (10)
		row1.getCell(8).getNumericCellValue should be (1)
		row1.getCell(9).getNumericCellValue should be (0)
		row1.getCell(10).getNumericCellValue should be (0)

		val row2 = assignmentSheet.getRow(2)
		row2.getCell(0).getStringCellValue should be ("test two")
		row2.getCell(1).getStringCellValue should be ("IN101")
		row2.getCell(2).getDateCellValue should be (dateTime(2013, 4, 10).toDate)
		row2.getCell(3).getNumericCellValue should be (29)
		row2.getCell(4).getNumericCellValue should be (29)
		row2.getCell(5).getNumericCellValue should be (0)
		row2.getCell(6).getNumericCellValue should be (5)
		row2.getCell(7).getNumericCellValue should be (0)
		row2.getCell(8).getNumericCellValue should be (0)
		row2.getCell(9).getNumericCellValue should be (29)
		row2.getCell(10).getNumericCellValue should be (1)

		val row3 = assignmentSheet.getRow(3)
		row3.getCell(0).getStringCellValue should be ("test three")
		row3.getCell(1).getStringCellValue should be ("IN101")
		row3.getCell(2).getDateCellValue should be (dateTime(2013, 5, 10).toDate)
		row3.getCell(3).getNumericCellValue should be (13)
		row3.getCell(4).getNumericCellValue should be (13)
		row3.getCell(5).getNumericCellValue should be (0)
		row3.getCell(6).getNumericCellValue should be (2)
		row3.getCell(7).getNumericCellValue should be (4)
		row3.getCell(8).getNumericCellValue should be (0.307692307692307692)
		row3.getCell(9).getNumericCellValue should be (9)
		row3.getCell(10).getNumericCellValue should be (0.6923076923076923)

		val row4 = assignmentSheet.getRow(4)
		row4.getCell(0).getStringCellValue should be ("test four")
		row4.getCell(1).getStringCellValue should be ("IN102")
		row4.getCell(2).getDateCellValue should be (dateTime(2013, 5, 31).toDate)
		row4.getCell(3).getNumericCellValue should be (35)
		row4.getCell(4).getNumericCellValue should be (35)
		row4.getCell(5).getNumericCellValue should be (0)
		row4.getCell(6).getNumericCellValue should be (7)
		row4.getCell(7).getNumericCellValue should be (7)
		row4.getCell(8).getNumericCellValue should be (0.2)
		row4.getCell(9).getNumericCellValue should be (28)
		row4.getCell(10).getNumericCellValue should be (0.8)

		val row5 = assignmentSheet.getRow(5)
		row5.getCell(0).getStringCellValue should be ("test five")
		row5.getCell(1).getStringCellValue should be ("IN102")
		row5.getCell(2).getDateCellValue should be (dateTime(2013, 8, 23).toDate)
		row5.getCell(3).getNumericCellValue should be (100)
		row5.getCell(4).getNumericCellValue should be (100)
		row5.getCell(5).getNumericCellValue should be (0)
		row5.getCell(6).getNumericCellValue should be (2)
		row5.getCell(7).getNumericCellValue should be (2)
		row5.getCell(8).getNumericCellValue should be (0.02)
		row5.getCell(9).getNumericCellValue should be (98)
		row5.getCell(10).getNumericCellValue should be (0.98)

		val row6 = assignmentSheet.getRow(6)
		row6.getCell(0).getStringCellValue should be ("test six")
		row6.getCell(1).getStringCellValue should be ("IN102")
		row6.getCell(2).getDateCellValue should be (dateTime(2013, 7, 1).toDate)
		row6.getCell(3).getNumericCellValue should be (73)
		row6.getCell(4).getNumericCellValue should be (73)
		row6.getCell(5).getNumericCellValue should be (24)
		row6.getCell(6).getNumericCellValue should be (0)
		row6.getCell(7).getNumericCellValue should be (65)
		row6.getCell(8).getNumericCellValue should be (0.890410958904109589)
		row6.getCell(9).getNumericCellValue should be (8)
		row6.getCell(10).getNumericCellValue should be (0.109589041095890410)

		val moduleSheet = command.generateModuleSheet(department)
		command.populateModuleSheet(moduleSheet)

		val row7 = moduleSheet.getRow(1)
		row7.getCell(0).getStringCellValue should be ("Module One")
		row7.getCell(1).getStringCellValue should be ("IN101")
		row7.getCell(2).getNumericCellValue should be (3)
		row7.getCell(3).getNumericCellValue should be (52)
		row7.getCell(4).getNumericCellValue should be (52)
		row7.getCell(5).getNumericCellValue should be (0)
		row7.getCell(6).getNumericCellValue should be (9)
		row7.getCell(7).getNumericCellValue should be (14)
		row7.getCell(8).getNumericCellValue should be (0.269230769230769230)
		row7.getCell(9).getNumericCellValue should be (38)
		row7.getCell(10).getNumericCellValue should be (0.730769230769230769)

		val row8 = moduleSheet.getRow(2)
		row8.getCell(0).getStringCellValue should be ("Module Two")
		row8.getCell(1).getStringCellValue should be ("IN102")
		row8.getCell(2).getNumericCellValue should be (3)
		row8.getCell(3).getNumericCellValue should be (208)
		row8.getCell(4).getNumericCellValue should be (208)
		row8.getCell(5).getNumericCellValue should be (24)
		row8.getCell(6).getNumericCellValue should be (9)
		row8.getCell(7).getNumericCellValue should be (74)
		row8.getCell(8).getNumericCellValue should be (0.3557692307692307692)
		row8.getCell(9).getNumericCellValue should be (134)
		row8.getCell(10).getNumericCellValue should be (0.6442307692307692307)

	}


	def getTestCommand() = {
		val command = new FeedbackReportCommand(department)
		command.startDate = dateTime(2013, 3, 1)
		command.endDate = dateTime(2013, 9, 30)
		command
	}

}
