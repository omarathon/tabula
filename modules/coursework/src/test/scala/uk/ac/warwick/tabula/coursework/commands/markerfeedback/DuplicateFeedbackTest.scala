package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import uk.ac.warwick.tabula.TestBase

class DuplicateFeedbackTest extends TestBase with MarkingWorkflowWorld{
	@Test
	def duplicateDetected {
		assignment.findFeedback("9876004").isDefined should be (true)
		assignment.findFullFeedback("9876004").isDefined should be (false)
	}
}
