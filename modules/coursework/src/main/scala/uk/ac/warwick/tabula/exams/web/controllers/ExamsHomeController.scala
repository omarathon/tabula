package uk.ac.warwick.tabula.exams.web.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{CurrentSITSAcademicYear, TaskBenchmarking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService

@Controller
@RequestMapping(Array("/exams"))
class ExamsHomeController extends ExamsController with CurrentSITSAcademicYear with TaskBenchmarking {

	@Autowired var moduleAndDepartmentService = Wire[ModuleAndDepartmentService]

	@RequestMapping
	def examsHome = {
		val ownedDepartments = benchmarkTask("Get owned departments") {
			moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Module.ManageAssignments)
		}

		val ownedModules = benchmarkTask("Get owned modules") {
			moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.ManageAssignments)
		}

		Mav("exams/home/view",
			"currentAcademicYear" -> academicYear,
			"ownedDepartments" -> ownedDepartments,
			"ownedModuleDepartments" -> ownedModules.map { _.adminDepartment }
		)
	}
}
