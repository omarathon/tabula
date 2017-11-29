package uk.ac.warwick.tabula.commands.coursework.marksmanagement

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.{Appliable, Describable}
import uk.ac.warwick.tabula.data.model.{DegreeType, Department}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

import scala.collection.JavaConverters._

class OpenAndCloseDepartmentsCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends OpenAndCloseDepartmentsCommandState
		with ModuleAndDepartmentServiceComponent
		with PopulateOpenAndCloseDepartmentsCommand {
			val moduleAndDepartmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
			moduleAndDepartmentService.allRootDepartments returns Seq()
	}

	trait OpenAndCloseDepartmentsWorld {
		val department: Department = Fixtures.department("in", "IT Services")
	}

	trait Fixture extends OpenAndCloseDepartmentsWorld {
		val now = new DateTime
		val command = new OpenAndCloseDepartmentsCommandInternal with CommandTestSupport
		command.moduleAndDepartmentService.allRootDepartments returns Seq(department)
//		command.termService.getTermFromDateIncludingVacations(any[BaseDateTime]) returns autumnTerm
		command.moduleAndDepartmentService.getDepartmentByCode(department.code) returns Some(department)
		val currentYear: AcademicYear = AcademicYear.forDate(now)
	}

	@Test
	def applyUndergrads() { new Fixture {
		command.populate()
		command.applyInternal() should be (DegreeType.Undergraduate)
		department.canUploadMarksToSitsForYear(currentYear, DegreeType.Undergraduate) should be (true)
	}}

	@Test
	def applyPostgradsClosed() { new Fixture{
		command.populate()
		command.pgMappings = Map(
			department.code -> DepartmentStateClosed.value
		).asJava
		command.updatePostgrads = true
		command.applyInternal() should be (DegreeType.Postgraduate)
		department.canUploadMarksToSitsForYear(currentYear, DegreeType.Postgraduate) should be (false)
		department.canUploadMarksToSitsForYear(command.previousAcademicYear, DegreeType.Postgraduate) should be(false)
	}}

	@Test
	def applyPostgradsOpenThisYearOnly() { new Fixture{
		command.populate()
		command.pgMappings = Map(
			department.code -> DepartmentStateThisYearOnly.value
		).asJava
		command.updatePostgrads = true
		command.applyInternal() should be (DegreeType.Postgraduate)
		department.canUploadMarksToSitsForYear(currentYear, DegreeType.Postgraduate) should be (true)
		department.canUploadMarksToSitsForYear(command.previousAcademicYear, DegreeType.Postgraduate) should be(false)
	}}

	@Test
	def populate() { new Fixture {
		command.currentAcademicYear should be (currentYear)
		command.previousAcademicYear should be (currentYear.-(1))
		command.ugMappings should be ('empty)
		command.pgMappings should be ('empty)
		command.populate()
		command.ugMappings should not be ('empty)
		command.pgMappings should not be ('empty)
		command.ugMappings.size should be (1)
		command.pgMappings.size should be (1)
	}}

	@Test
	def permssions {
		val command = new OpenAndCloseDepartmentsCommandPermissions with CommandTestSupport
		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
		verify(checking, times(1)).PermissionCheck(Permissions.Marks.MarksManagement)
	}

	@Test
	def glueEverythingTogether() {
			val command = OpenAndCloseDepartmentsCommand()
			command should be (anInstanceOf[Appliable[DegreeType]])
			command should be (anInstanceOf[OpenAndCloseDepartmentsCommandPermissions])
			command should be (anInstanceOf[OpenAndCloseDepartmentsCommandState])
			command should be (anInstanceOf[Describable[DegreeType]])
		}
}