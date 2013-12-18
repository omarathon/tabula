package uk.ac.warwick.tabula.coursework.web.controllers

import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.coursework.web.controllers.admin.FeedbackSummaryController

class FeedbackControllersTest extends TestBase with Mockito {
	val controller = new FeedbackSummaryController
	val command = mock[Appliable[Option[Feedback]]]
	val feedback = new Feedback
	command.apply() returns Some(feedback)

	@Test
	def display() {
		controller.showForm(command) should be (Mav("admin/assignments/feedback/read_only",
			"embedded" -> true,
			"renderLayout" -> "nonav",
			"feedback" -> feedback))
	}
}