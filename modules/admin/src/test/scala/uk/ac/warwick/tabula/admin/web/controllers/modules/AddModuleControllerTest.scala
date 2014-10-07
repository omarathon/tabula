package uk.ac.warwick.tabula.admin.web.controllers.modules

import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, TestBase, Mockito}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{Module, Department}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.admin.web.Routes

class AddModuleControllerTest extends TestBase with Mockito {

	val controller = new AddModuleController

	@Test def createsCommand {
		val department = Fixtures.department("in")

		val command = controller.command(department)

		command should be (anInstanceOf[Appliable[Department]])
	}

	@Test(expected = classOf[ItemNotFoundException]) def requiresDepartment {
		controller.command(null)
	}

	@Test def form {
		val department = Fixtures.department("in")

		val mav = controller.showForm(department)
		mav.viewName should be ("admin/modules/add/form")
		mav.toModel("department") should be (department)
	}

	@Test def submit {
		val department = Fixtures.department("in")
		val command = mock[Appliable[Module]]

		val module = Fixtures.module("in101")
		module.adminDepartment = department

		command.apply() returns (module)

		val errors = new BindException(command, "command")

		val mav = controller.submit(command, errors, department)
		mav.viewName should be (s"redirect:${Routes.module(module)}")

		there was one (command).apply()
	}

	@Test def submitValidationError {
		val department = Fixtures.department("in")
		val command = mock[Appliable[Module]]

		val errors = new BindException(command, "command")
		errors.reject("fail")

		val mav = controller.submit(command, errors, department)
		mav.viewName should be ("admin/modules/add/form")
		mav.toModel("department") should be (department)

		there was no (command).apply()
	}

}
