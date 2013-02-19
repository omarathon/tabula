package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import uk.ac.warwick.tabula.AppContextTestBase

class DuplicateFeedbackTest extends AppContextTestBase with MarkingWorkflowWorld{
	@Test
	def duplicateDetected {
		assignment.findFeedback("9876004").isDefined should be (true)
		assignment.findFullFeedback("9876004").isDefined should be (false)
	}
}
