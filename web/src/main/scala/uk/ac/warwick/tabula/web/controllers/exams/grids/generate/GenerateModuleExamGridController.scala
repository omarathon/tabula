package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import javax.validation.Valid
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.jobs.scheduling.ImportMembersJob
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

import scala.collection.JavaConverters._

object GenerateModuleExamGridMappingParameters {
	final val selectModule = "selectModule"
	final val previewAndDownload = "previewAndDownload"
	final val excel = "excel"
}

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/module/generate"))
class GenerateModuleExamGridController extends ExamsController
	with DepartmentScopedController with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent with AutowiringJobServiceComponent
	with AutowiringCourseAndRouteServiceComponent with AutowiringModuleRegistrationServiceComponent
	with ExamModuleGridDocumentsController
	with TaskBenchmarking {


	type SelectModuleExamCommand = Appliable[ModuleExamGridResult] with GenerateModuleExamGridCommandRequest with GenerateModuleExamGridCommandState

	override val departmentPermission: Permission = Permissions.Department.ExamGrids

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	validatesSelf[SelfValidating]


	@ModelAttribute("modules")
	def fetchModules(@PathVariable department: Department): Set[Module] = {
		val modulesWithPermission = moduleAndDepartmentService.modulesWithPermission(user, Permissions.Department.ExamGrids)
		val canManageDepartment: Boolean = securityService.can(user, Permissions.Department.ExamGrids, department)
		if (canManageDepartment) department.modules.asScala.toSet
		else modulesWithPermission
	}

	private def commonCrumbs(view: Mav, department: Department, academicYear: AcademicYear): Mav =
		view.crumbs(Breadcrumbs.Grids.Home, Breadcrumbs.Grids.Department(department, academicYear))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.exams.Grids.moduleGenerate(department, year)): _*)

	@ModelAttribute("selectModuleExamCommand")
	def selectModuleExamCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateModuleExamGridCommand(mandatory(department), mandatory(academicYear))


	@ModelAttribute("GenerateModuleExamGridMappingParameters")
	def params = GenerateModuleExamGridMappingParameters

	@RequestMapping(method = Array(GET, POST))
	def selectModuleRender(
		@ModelAttribute("selectModuleExamCommand") selectModuleExamCommand: SelectModuleExamCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		commonCrumbs(
			Mav("exams/grids/module/generate/selectModule"),
			department,
			academicYear
		)
	}


	@RequestMapping(method = Array(POST), params = Array(GenerateModuleExamGridMappingParameters.selectModule))
	def selectExamModuleSubmit(
		@Valid @ModelAttribute("selectModuleExamCommand") selectModuleExamCommand: SelectModuleExamCommand,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam allRequestParams: JMap[String, String]
	): Mav = {
		val students = selectModuleExamCommand.apply().gridStudentDetailRecords
		if (students.isEmpty) {
			errors.reject("examGrid.noStudents")
			selectModuleRender(selectModuleExamCommand, department, academicYear)
		} else {
			val jobInstance = jobService.add(Some(user), ImportMembersJob(students.map(_.moduleRegistration.studentCourseDetails.student.universityId)))
			checkJobProgress(jobInstance.id, selectModuleExamCommand, department, academicYear)
		}
	}


	private def checkJobProgress(
		jobId: String,
		selectModuleExamCommand: SelectModuleExamCommand,
		department: Department,
		academicYear: AcademicYear
	) = {
		val jobInstance = jobService.getInstance(jobId)
		if (jobInstance.isDefined && !jobInstance.get.finished) {
			val moduleGridResult = selectModuleExamCommand.apply()
			val studentLastImportDates = moduleGridResult.gridStudentDetailRecords.map { e =>
				(e.name, e.lastImportDate.getOrElse(new DateTime(0)))
			}.sortBy(_._2)
			commonCrumbs(
				Mav("exams/grids/module/generate/jobProgress",
					"jobId" -> jobId,
					"module" -> selectModuleExamCommand.module,
					"passMark" -> ProgressionService.modulePassMark(selectModuleExamCommand.module.degreeType),
					"entities" -> moduleGridResult.gridStudentDetailRecords,
					"studentCount" -> moduleGridResult.gridStudentDetailRecords.map(_.universityId).distinct.size,
					"componentInfo" -> moduleGridResult.upstreamAssessmentGroupAndSequenceAndOccurrencesWithComponentName,
					"jobProgress" -> jobInstance.get.progress,
					"jobStatus" -> jobInstance.get.status,
					"studentLastImportDates" -> studentLastImportDates
				),
				department,
				academicYear
			)
		} else {
			previewAndDownloadRender(
				selectModuleExamCommand,
				department,
				academicYear,
				jobId
			)
		}
	}

	@RequestMapping(method = Array(POST), value = Array("/progress"))
	def jobProgress(@RequestParam jobId: String): Mav = {
		jobService.getInstance(jobId).map(jobInstance =>
			Mav(new JSONView(Map(
				"id" -> jobInstance.id,
				"status" -> jobInstance.status,
				"progress" -> jobInstance.progress,
				"finished" -> jobInstance.finished
			))).noLayout()
		).getOrElse(throw new ItemNotFoundException())
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.previewAndDownload))
	def previewAndDownload(
		@Valid @ModelAttribute("selectModuleExamCommand") selectModuleExamCommand: SelectModuleExamCommand,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam jobId: String
	): Mav = {
		if (errors.hasErrors) {
			throw new IllegalArgumentException
		}
		previewAndDownloadRender(
			selectModuleExamCommand,
			department,
			academicYear,
			jobId
		)
	}

	private def previewAndDownloadRender(
		selectModuleExamCommand: SelectModuleExamCommand,
		department: Department,
		academicYear: AcademicYear,
		jobId: String
	): Mav = {

		val moduleGridResult = selectModuleExamCommand.apply()
		val oldestImport = benchmarkTask("oldestImport") {
			moduleGridResult.gridStudentDetailRecords.flatMap(_.lastImportDate).reduceOption((a, b) => if (a.isBefore(b)) a else b)
		}
		val mavObjects = Map(
			"oldestImport" -> oldestImport,
			"entities" -> moduleGridResult.gridStudentDetailRecords,
			"studentCount" -> moduleGridResult.gridStudentDetailRecords.map(_.universityId).distinct.size,
			"generatedDate" -> DateTime.now,
			"jobId" -> jobId,
			"module" -> selectModuleExamCommand.module,
			"passMark" -> ProgressionService.modulePassMark(selectModuleExamCommand.module.degreeType),
			"componentInfo" -> moduleGridResult.upstreamAssessmentGroupAndSequenceAndOccurrencesWithComponentName
		)
		commonCrumbs(
			Mav("exams/grids/module/generate/preview", mavObjects),
			department,
			academicYear
		)
	}
}
