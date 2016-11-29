package uk.ac.warwick.tabula.commands.exams.grids

import org.mockito.ArgumentCaptor
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{CoreRequiredModule, Department, Module, Route}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ModuleAndDepartmentServiceComponent, ModuleRegistrationService, ModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

import collection.JavaConverters._

class GenerateExamGridSetCoreRequiredModulesCommandTest extends TestBase with Mockito {

	val thisDepartment: Department = Fixtures.department("its")
	val thisAcademicYear = AcademicYear(2014)
	val thisRoute: Route = Fixtures.route("its1")
	val thisYearOfStudy = 2
	val module1: Module = Fixtures.module("its01")
	val module2: Module = Fixtures.module("its02")

	@Test
	def populate(): Unit = {
		val coreRequiredModule = new CoreRequiredModule(thisRoute, thisAcademicYear, thisYearOfStudy, module1)
		val command = new PopulateGenerateExamGridSetCoreRequiredModulesCommand()
			with GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest
			with GenerateExamGridSelectCourseCommandRequest with ModuleRegistrationServiceComponent with ModuleAndDepartmentServiceComponent {
			override val department: Department = thisDepartment
			override val academicYear: AcademicYear = thisAcademicYear
			override val moduleRegistrationService: ModuleRegistrationService = smartMock[ModuleRegistrationService]
			override val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
		}
		command.route = thisRoute
		command.yearOfStudy = thisYearOfStudy
		command.moduleRegistrationService.findCoreRequiredModules(thisRoute, thisAcademicYear, thisYearOfStudy) returns Seq(coreRequiredModule)
		command.populate()
		verify(command.moduleRegistrationService, times(1)).findCoreRequiredModules(thisRoute, thisAcademicYear, thisYearOfStudy)
		command.modules.size should be (1)
		command.modules.asScala.head should be (module1)
	}

	@Test
	def validate(): Unit = {
		val command = new GenerateExamGridSetCoreRequiredModulesCommandValidation()
			with GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest
			with GenerateExamGridSelectCourseCommandRequest with ModuleRegistrationServiceComponent with ModuleAndDepartmentServiceComponent {
			override val department: Department = thisDepartment
			override val academicYear: AcademicYear = thisAcademicYear
			override val moduleRegistrationService: ModuleRegistrationService = smartMock[ModuleRegistrationService]
			override val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
		}
		command.route = thisRoute
		command.yearOfStudy = thisYearOfStudy
		command.moduleAndDepartmentService.findByRouteYearAcademicYear(thisRoute, thisYearOfStudy, thisAcademicYear) returns Seq(module1)

		val duffModule = Fixtures.module("its2")
		command.modules.add(duffModule)

		val errors = new BindException(command, "command")
		command.validate(errors)
		verify(command.moduleAndDepartmentService, times(1)).findByRouteYearAcademicYear(thisRoute, thisYearOfStudy, thisAcademicYear)
		errors.hasErrors should be {true}
		errors.getAllErrors.get(0).getArguments.apply(0).asInstanceOf[String].contains(duffModule.code) should be {true}

		command.modules.clear()
		command.modules.add(module1)
		val errors1 = new BindException(command, "command")
		command.validate(errors1)
		verify(command.moduleAndDepartmentService, times(1)).findByRouteYearAcademicYear(thisRoute, thisYearOfStudy, thisAcademicYear)
		errors1.hasErrors should be {false}
	}

	@Test
	def apply(): Unit = {
		val coreRequiredModule = new CoreRequiredModule(thisRoute, thisAcademicYear, thisYearOfStudy, module1)
		val command = new GenerateExamGridSetCoreRequiredModulesCommandInternal(thisDepartment, thisAcademicYear)
			with GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest
			with GenerateExamGridSelectCourseCommandRequest with ModuleRegistrationServiceComponent with ModuleAndDepartmentServiceComponent {
			override val moduleRegistrationService: ModuleRegistrationService = smartMock[ModuleRegistrationService]
			override val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
		}
		command.route = thisRoute
		command.yearOfStudy = thisYearOfStudy
		command.moduleRegistrationService.findCoreRequiredModules(thisRoute, thisAcademicYear, thisYearOfStudy) returns Seq(coreRequiredModule)

		command.modules.add(module2) // New module, not containing existing module

		val result = command.applyInternal()
		verify(command.moduleRegistrationService, times(1)).findCoreRequiredModules(thisRoute, thisAcademicYear, thisYearOfStudy)
		verify(command.moduleRegistrationService, times(1)).delete(coreRequiredModule)
		val argument: ArgumentCaptor[CoreRequiredModule]  = ArgumentCaptor.forClass(classOf[CoreRequiredModule])
		verify(command.moduleRegistrationService, times(1)).saveOrUpdate(argument.capture())
		argument.getValue.route should be (thisRoute)
		argument.getValue.academicYear should be (thisAcademicYear)
		argument.getValue.yearOfStudy should be (thisYearOfStudy)
		argument.getValue.module should be (module2)
		result.size should be (1)
		result.head should be (argument.getValue)
	}

}
