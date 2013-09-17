package uk.ac.warwick.tabula.coursework.web.controllers

import uk.ac.warwick.tabula.{CurrentUser, Mockito, TestBase}
import uk.ac.warwick.tabula.coursework.web.controllers.admin.{OnlineFeedbackFormController, OnlineFeedbackController}
import uk.ac.warwick.tabula.coursework.commands.feedback.{OnlineFeedbackFormCommandTestSupport, OnlineFeedbackFormCommand, OnlineFeedbackCommandTestSupport, OnlineFeedbackCommand}
import uk.ac.warwick.tabula.data.model.{StudentMember, Department, Module, Assignment}
import uk.ac.warwick.userlookup.User
import org.mockito.Mockito._
import org.springframework.validation.Errors


class OnlineFeedbackControllerTestextends extends TestBase {

	trait Fixture {
		val department = new Department
		department.code = "hr"
		val module = new Module
		module.department = department
		module.code = "hrn101"
		val assignment = new Assignment
		assignment.module = module
		assignment.name = "Herons are evil"
		val command = new OnlineFeedbackCommand(module, assignment) with OnlineFeedbackCommandTestSupport
	}

	@Test def controllerShowsList() {
		new Fixture {
			val controller = new OnlineFeedbackController
			val mav = controller.showTable(command, null)
			mav.map("assignment") should be(assignment)
			mav.map("command") should be(command)
			mav.map("studentFeedbackGraphs") should be(Seq())
			mav.viewName should be ("admin/assignments/feedback/online_framework")
		}
	}

}

class OnlineFeedbackFormControllerTest extends TestBase with Mockito {

	trait Fixture {
		val department = new Department
		department.code = "hr"
		val module = new Module
		module.department = department
		module.code = "hrn101"
		val assignment = new Assignment
		assignment.module = module
		assignment.name = "Herons are evil"
		val student = new StudentMember("student")
		val marker = new User("marker")
		val currentUser = new CurrentUser(marker, marker)
		val command = new OnlineFeedbackFormCommand(module, assignment, student, currentUser)
			with OnlineFeedbackFormCommandTestSupport

		val controller = new OnlineFeedbackFormController
	}

	@Test def controllerShowsForm() {
		new Fixture {
			val mav = controller.showForm(command, null)
			mav.map("command") should be(command)
			mav.viewName should be ("admin/assignments/feedback/online_feedback")
		}
	}

	@Test def controllerShowsFormIfErrors() {
		new Fixture {
			val errors = mock[Errors]
			when(errors.hasErrors) thenReturn(true)
			val mav = controller.submit(command, errors)
			mav.map("command") should be(command)
			mav.viewName should be ("admin/assignments/feedback/online_feedback")
		}
	}

	@Test def controllerAppliesCommand() {
		new Fixture {
			val errors = mock[Errors]
			when(errors.hasErrors) thenReturn(false)
			val mav = controller.submit(command, errors)
			mav.viewName should be ("ajax_success")
			mav.map("renderLayout") should be("none")
		}
	}

}

