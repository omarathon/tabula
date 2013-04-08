package uk.ac.warwick.tabula.coursework.commands.departments

import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.userlookup.User
import org.junit.Ignore
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFSheet

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
	def assignmentSheetTest() {
		val command = getTestCommand()
		command.assignmentMembershipService = assignmentMembershipService
		command.auditEventQueryMethods = auditEventQueryMethods		
		command.buildAssignmentData()
		
		val sheet = command.generateAssignmentSheet(department)
		command.populateAssignmentSheet(sheet)		
		
		val row = sheet.getRow(1)
		row.getCell(0).getStringCellValue() should be ("test one")
		
		val thing = sheet
		
		
	}

	
	def getTestCommand() = {
		val command = new FeedbackReportCommand(department)
		command.startDate = dateTime(2013, 3, 1)
		command.endDate = dateTime(2013, 9, 30)
		command
	}

}
