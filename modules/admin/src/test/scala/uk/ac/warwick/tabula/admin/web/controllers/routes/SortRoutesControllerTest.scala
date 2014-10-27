package uk.ac.warwick.tabula.admin.web.controllers.routes

import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, TestBase, Mockito}
import uk.ac.warwick.tabula.admin.commands.routes.SortRoutesCommandState
import uk.ac.warwick.tabula.data.model.{Route, Department}
import uk.ac.warwick.tabula.commands.{Appliable, GroupsObjects}
import uk.ac.warwick.tabula.admin.web.Routes
import org.springframework.validation.BindException

class SortRoutesControllerTest extends TestBase with Mockito {

	val controller = new SortRoutesController

	@Test def createsCommand() {
		val department = Fixtures.department("in")

		val command = controller.command(department)

		command should be (anInstanceOf[Appliable[Unit]])
	}

	@Test(expected = classOf[ItemNotFoundException]) def requiresDepartment() {
		controller.command(null)
	}

	class CountingCommand(val department: Department) extends Appliable[Unit] with GroupsObjects[Route, Department] with SortRoutesCommandState {
		var populateCount = 0
		var sortCount = 0
		var applyCount = 0

		def populate() { populateCount += 1 }
		def sort() { sortCount += 1 }
		def apply() { applyCount += 1 }
	}

	trait Fixture {
		val department = Fixtures.department("in")
		val subDepartment = Fixtures.department("in-ug")
		department.children.add(subDepartment)
		subDepartment.parent = department
	}

	@Test def formOnParent() { new Fixture {
		val command = new CountingCommand(department)
		val mav = controller.showForm(command)
		mav.viewName should be ("admin/routes/arrange/form")
		mav.toModel should be ('empty)

		command.populateCount should be (1)
		command.sortCount should be (1)
		command.applyCount should be (0)
	}}

	@Test def formOnChild() { new Fixture {
		val command = new CountingCommand(subDepartment)
		val mav = controller.showForm(command)
		mav.viewName should be (s"redirect:${Routes.department.sortRoutes(department)}")
		mav.toModel should be ('empty)

		command.populateCount should be (1)
		command.sortCount should be (1)
		command.applyCount should be (0)
	}}

	@Test def formOnLoneDepartment() { new Fixture {
		val command = new CountingCommand(Fixtures.department("xx"))
		val mav = controller.showForm(command)
		mav.viewName should be ("admin/routes/arrange/form")
		mav.toModel should be ('empty)

		command.populateCount should be (1)
		command.sortCount should be (1)
		command.applyCount should be (0)
	}}

	@Test def submitParent() { new Fixture {
		val command = new CountingCommand(department)
		val errors = new BindException(command, "command")

		val mav = controller.submit(command, errors)
		mav.viewName should be ("admin/routes/arrange/form")
		mav.toModel("saved").toString should be ("true")

		command.populateCount should be (0)
		command.sortCount should be (1)
		command.applyCount should be (1)
	}}

	@Test def submitValidationErrors() { new Fixture {
		val command = new CountingCommand(department)
		val errors = new BindException(command, "command")
		errors.reject("fail")

		val mav = controller.submit(command, errors)
		mav.viewName should be ("admin/routes/arrange/form")
		mav.toModel should be ('empty)

		command.populateCount should be (0)
		command.sortCount should be (1)
		command.applyCount should be (0)
	}}

}
