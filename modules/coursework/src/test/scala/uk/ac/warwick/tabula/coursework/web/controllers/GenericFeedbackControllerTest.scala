package uk.ac.warwick.tabula.coursework.web.controllers

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.coursework.commands.feedback.GenericFeedbackCommand
import uk.ac.warwick.tabula.coursework.web.controllers.admin.GenericFeedbackController
import org.springframework.validation.Errors

class GenericFeedbackControllerTest extends TestBase with Mockito {

	trait Fixture {
		val module = new Module
		val assignment = new Assignment
		assignment.module = module

		val command = mock[GenericFeedbackCommand]
		val controller = new GenericFeedbackController
	}

	@Test def controllerShowsForm() {
		new Fixture {
			val mav = controller.showForm(command, null)
			mav.map("command") should be(command)
			mav.viewName should be ("admin/assignments/feedback/generic_feedback")
		}
	}

	@Test def controllerAppliesCommand() {
		new Fixture {
			val errors = mock[Errors]
			val mav = controller.submit(command, errors)

			there was one(command).apply()

			mav.viewName should be ("ajax_success")
			mav.map("renderLayout") should be("none")
		}
	}

}
