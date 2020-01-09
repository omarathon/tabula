package uk.ac.warwick.tabula.web.controllers.admin.department

import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{ItemNotFoundException, Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import org.springframework.validation.BindException

class AddSubDepartmentControllerTest extends TestBase with Mockito {

  val controller = new AddSubDepartmentController

  @Test def createsCommand: Unit = {
    val department = Fixtures.department("in")

    val command = controller.command(department)
    command.parent should be(department)

    command should be(anInstanceOf[Appliable[Department]])
  }

  @Test(expected = classOf[ItemNotFoundException]) def requiresDepartment: Unit = {
    controller.command(null)
  }

  @Test def form: Unit = {
    controller.showForm().viewName should be("admin/department/add/form")
  }

  @Test def submit(): Unit = {
    val subDepartment = Fixtures.department("in-ug", isImportDepartment = false)
    val command = mock[Appliable[Department]]
    command.apply() returns (subDepartment)

    val errors = new BindException(command, "command")

    controller.submit(command, errors).viewName should be(s"redirect:${Routes.admin.department(subDepartment)}")

    verify(command, times(1)).apply()
  }

  @Test def validation: Unit = {
    val command = mock[Appliable[Department]]
    val errors = new BindException(command, "command")
    errors.reject("error")

    controller.submit(command, errors).viewName should be("admin/department/add/form")

    verify(command, times(0)).apply()
  }

}
