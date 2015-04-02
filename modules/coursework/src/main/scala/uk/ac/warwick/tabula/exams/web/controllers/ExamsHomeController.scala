package uk.ac.warwick.tabula.exams.web.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{CurrentSITSAcademicYear, TaskBenchmarking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentService, ModuleAndDepartmentService}

@Controller
@RequestMapping(Array("/exams"))
class ExamsHomeController extends ExamsController with CurrentSITSAcademicYear with TaskBenchmarking {

	@Autowired var moduleAndDepartmentService = Wire[ModuleAndDepartmentService]
	@Autowired var assessmentService = Wire[AssessmentService]

	@RequestMapping
	def examsHome = {
		val ownedDepartments = benchmarkTask("Get owned departments") {
			moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Module.ManageAssignments)
		}

		val ownedModules = benchmarkTask("Get owned modules") {
			moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.ManageAssignments)
		}

		val examsForMarking = benchmarkTask("Get exams for marking") {
			assessmentService.getExamsWhereMarker(user.apparentUser).sortBy(_.module.code)
		}

		Mav("exams/home/view",
			"currentAcademicYear" -> academicYear,
			"examsForMarking" -> examsForMarking,
			"ownedDepartments" -> ownedDepartments,
			"ownedModuleDepartments" -> ownedModules.map { _.adminDepartment }
		)
	}
}
