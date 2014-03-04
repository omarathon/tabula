package uk.ac.warwick.tabula.admin.web.controllers.modules

import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, TestBase, Mockito}
import uk.ac.warwick.tabula.admin.commands.modules.SortModulesCommandState
import uk.ac.warwick.tabula.data.model.{Module, Department}
import uk.ac.warwick.tabula.commands.{Appliable, GroupsObjects}
import uk.ac.warwick.tabula.admin.web.Routes
import org.springframework.validation.BindException

class SortModulesControllerTest extends TestBase with Mockito {

	val controller = new SortModulesController

	@Test def createsCommand {
		val department = Fixtures.department("in")

		val command = controller.command(department)

		command should be (anInstanceOf[Appliable[Unit]])
	}

	@Test(expected = classOf[ItemNotFoundException]) def requiresDepartment {
		controller.command(null)
	}

	class CountingCommand(val department: Department) extends Appliable[Unit] with GroupsObjects[Module, Department] with SortModulesCommandState {
		var populateCount = 0
		var sortCount = 0
		var applyCount = 0

		def populate() { populateCount += 1 }
		def sort() { sortCount += 1 }
		def apply() { applyCount += 1 }
	}

	trait Fixture {
		val department = Fixtures.department("in")
		val command = new CountingCommand(department)
	}

	trait ParentFixture extends Fixture {
		val parent = Fixtures.department("in-parent")
		department.parent = parent
	}

	@Test def form { new Fixture {
		val mav = controller.showForm(command)
		mav.viewName should be ("admin/modules/arrange/form")
		mav.toModel should be ('empty)

		command.populateCount should be (1)
		command.sortCount should be (1)
		command.applyCount should be (0)
	}}

	@Test def formWithParent { new ParentFixture {
		val mav = controller.showForm(command)
		mav.viewName should be (s"redirect:${Routes.department.sortModules(parent)}")
		mav.toModel should be ('empty)

		command.populateCount should be (1)
		command.sortCount should be (1)
		command.applyCount should be (0)
	}}

	@Test def submit { new Fixture {
		val errors = new BindException(command, "command")

		val mav = controller.submit(command, errors)
		mav.viewName should be ("admin/modules/arrange/form")
		mav.toModel("saved").toString should be ("true")

		command.populateCount should be (0)
		command.sortCount should be (1)
		command.applyCount should be (1)
	}}

	@Test def submitValidationErrors { new Fixture {
		val errors = new BindException(command, "command")
		errors.reject("fail")

		val mav = controller.submit(command, errors)
		mav.viewName should be ("admin/modules/arrange/form")
		mav.toModel should be ('empty)

		command.populateCount should be (0)
		command.sortCount should be (1)
		command.applyCount should be (0)
	}}

}
