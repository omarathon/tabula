package uk.ac.warwick.tabula.web.controllers.cm2.admin.department

import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.services.permissions.PermissionsServiceComponent
import uk.ac.warwick.tabula.web.Routes

class DisplaySettingsControllerTest extends TestBase with Mockito {

	val controller = new DisplaySettingsController
	controller.relationshipService = smartMock[RelationshipService]

	@Test def createsCommand {
		val department = Fixtures.department("in")

		val command = controller.displaySettingsCommand(department)

		command should be (anInstanceOf[Appliable[Department]])
		command should be (anInstanceOf[PopulateOnForm])
	}

	@Test(expected = classOf[ItemNotFoundException]) def requiresDepartment {
		controller.displaySettingsCommand(null)
	}

	@Test def form {
		val department = Fixtures.department("in")

		var populateCalledCount = 0
		val command = new Appliable[Department] with PopulateOnForm with PermissionsServiceComponent {
			val permissionsService = null
			def populate() {
				populateCalledCount += 1
			}
			def apply(): Null = {
				fail("Should not be called")
				null
			}
		}

		val mav = controller.initialView(department, command)
		mav.viewName should be ("${cm2.prefix}/admin/display-settings")
		mav.toModel("department") should be (department)
		mav.toModel("returnTo") should be ("")

		populateCalledCount should be (1)
	}

	@Test def submit {
		val department = Fixtures.department("in")

		var populateCalledCount = 0
		var applyCalledCount = 0
		val command = new Appliable[Department] with PopulateOnForm with PermissionsServiceComponent {
			val permissionsService = null

			def populate() {
				populateCalledCount += 1
			}
			def apply(): Department = {
				applyCalledCount += 1
				department
			}
		}

		val errors = new BindException(command, "command")

		val academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		val mav = controller.saveSettings(command, errors, department)
		mav.viewName should be (s"redirect:${Routes.cm2.admin.department(department,academicYear)}")
		mav.toModel should be ('empty)

		populateCalledCount should be (0)
		applyCalledCount should be (1)
	}

	@Test def submitValidationErrors {
		val department = Fixtures.department("in")

		var populateCalledCount = 0
		var applyCalledCount = 0
		val command = new Appliable[Department] with PopulateOnForm with PermissionsServiceComponent {
			val permissionsService = null
			def populate() {
				populateCalledCount += 1
			}
			def apply(): Department = {
				applyCalledCount += 1
				department
			}
		}

		val errors = new BindException(command, "command")
		errors.reject("fail")

		val mav = controller.saveSettings(command, errors, department)
		mav.viewName should be ("${cm2.prefix}/admin/display-settings")
		mav.toModel("department") should be (department)
		mav.toModel("returnTo") should be ("")

		populateCalledCount should be (0)
		applyCalledCount should be (0)
	}

}
