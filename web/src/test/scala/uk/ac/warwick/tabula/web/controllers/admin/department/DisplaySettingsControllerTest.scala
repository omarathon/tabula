package uk.ac.warwick.tabula.web.controllers.admin.department

import org.springframework.ui.ModelMap
import org.springframework.validation.{BindException, Errors}
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap
import uk.ac.warwick.tabula.commands.admin.department.{DisplaySettingsCommand, DisplaySettingsCommandRequest, DisplaySettingsCommandState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}

class DisplaySettingsControllerTest extends TestBase with Mockito {

  val controller = new DisplaySettingsController
  controller.relationshipService = mock[RelationshipService]

  @Test(expected = classOf[ItemNotFoundException]) def requiresDepartment(): Unit = {
    controller.displaySettingsCommand(null)
  }

  private abstract class TestDisplaySettingsCommandInternal(val department: Department) extends Appliable[Department] with DisplaySettingsCommandState

  @Test def form(): Unit = {
    controller.formView should be("admin/display-settings")
    controller.returnTo should be ("")
  }

  @Test def submit(): Unit = {
    val department = Fixtures.department("in")

    var applyCalledCount = 0
    val command: DisplaySettingsCommand.Command = new TestDisplaySettingsCommandInternal(department) with DisplaySettingsCommandRequest with SelfValidating {
      override def apply(): Department = {
        applyCalledCount += 1
        department
      }

      override def validate(errors: Errors): Unit = fail("Should not be called")
    }

    val errors = new BindException(command, "command")
    val redirectAttributes = new RedirectAttributesModelMap
    val model = new ModelMap

    controller.saveSettings(command, errors, department, model)(redirectAttributes) should be(s"redirect:${Routes.admin.department(department)}")
    model.isEmpty should be (true)
    redirectAttributes.getFlashAttributes.get("flash__success") should be ("flash.departmentSettings.saved")

    applyCalledCount should be(1)
  }

  @Test def submitValidationErrors(): Unit = {
    val department = Fixtures.department("in")

    var applyCalledCount = 0
    val command: DisplaySettingsCommand.Command = new TestDisplaySettingsCommandInternal(department) with DisplaySettingsCommandRequest with SelfValidating {
      override def apply(): Department = {
        applyCalledCount += 1
        department
      }

      override def validate(errors: Errors): Unit = {
        errors.reject("fail")
      }
    }

    val errors = new BindException(command, "command")
    val redirectAttributes = new RedirectAttributesModelMap
    val model = new ModelMap

    command.validate(errors) // simulates @Valid on controller
    controller.saveSettings(command, errors, department, model)(redirectAttributes) should be("admin/display-settings")
    controller.returnTo should be ("")
    model.get("flash__error") should be ("flash.hasErrors")
    redirectAttributes.getFlashAttributes.isEmpty should be (true)

    applyCalledCount should be(0)
  }

}
