package uk.ac.warwick.tabula.commands.cm2.marksmanagement

import org.springframework.transaction.annotation.Propagation
import uk.ac.warwick.tabula.commands.{Appliable, Describable}
import uk.ac.warwick.tabula.data.TransactionalComponent
import uk.ac.warwick.tabula.data.model.{DegreeType, Department}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class MarksOpenAndCloseDepartmentsCommandTest extends TestBase with Mockito {

  trait CommandTestSupport extends MarksOpenAndCloseDepartmentsRequest with ModuleAndDepartmentServiceComponent with TransactionalComponent {
    override val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
    override def transactional[A](readOnly: Boolean, propagation: Propagation)(f: => A): A = f
  }

  trait OpenAndCloseDepartmentsWorld {
    val department: Department = Fixtures.department("in", "IT Services")
    department.modules.add(Fixtures.module("in101"))
  }

  trait Fixture extends OpenAndCloseDepartmentsWorld {
    val command: MarksOpenAndCloseDepartmentsCommandInternal with CommandTestSupport =
      new MarksOpenAndCloseDepartmentsCommandInternal with CommandTestSupport {}

    command.moduleAndDepartmentService.allRootDepartments returns Seq(department)
    command.moduleAndDepartmentService.getDepartmentByCode(department.code) returns Some(department)

    command.populate()
  }

  @Test
  def apply(): Unit = new Fixture {
    command.state.get("in").uploadCourseworkMarksToSits = true
    command.state.get("in").openForCurrentYearUG = true

    command.applyInternal() should be(Seq(department))

    department.uploadCourseworkMarksToSits should be (true)
    department.canUploadMarksToSitsForYear(AcademicYear.now(), DegreeType.Undergraduate) should be (true)

    verify(command.moduleAndDepartmentService, times(1)).saveOrUpdate(department)
  }

  @Test
  def permssions(): Unit = {
    val command = new MarksOpenAndCloseDepartmentsCommandPermissions {}
    val checking = smartMock[PermissionsChecking]
    command.permissionsCheck(checking)
    verify(checking, times(1)).PermissionCheck(Permissions.Marks.MarksManagement)
  }
}
