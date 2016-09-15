package uk.ac.warwick.tabula.web.controllers.coursework

import uk.ac.warwick.tabula.{AppContextTestBase, Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.commands.coursework.feedback.GenericFeedbackCommand
import uk.ac.warwick.tabula.web.controllers.coursework.admin.OldGenericFeedbackController
import org.springframework.validation.Errors

class OldGenericFeedbackControllerTest extends AppContextTestBase with Mockito {

	trait Fixture {
		val department = Fixtures.department("hz", "Heron studies")
		val module = new Module
		module.code = "hn101"
		module.adminDepartment = department
		val assignment = new Assignment
		assignment.module = module

		val command = mock[GenericFeedbackCommand]
	}

	@Test def controllerShowsForm() {
		new Fixture {
			val controller = new OldGenericFeedbackController
			val mav = controller.showForm(assignment, command, null)
			mav.map("command") should be(command)
			mav.viewName should be ("coursework/admin/assignments/feedback/generic_feedback")
		}
	}

	@Test def controllerAppliesCommand() {
		new Fixture {
			val controller = new OldGenericFeedbackController { override val ajax = true }
			val errors = mock[Errors]

			val mav = controller.submit(assignment, command, errors)

			verify(command, times(1)).apply()

			mav.viewName should be ("ajax_success")
		}
	}

}
