package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import org.springframework.web.servlet.View
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.exams.grids._
import uk.ac.warwick.tabula.commands.{Appliable, FilterStudentsOrRelationships, SelfValidating}
import uk.ac.warwick.tabula.data.AutowiringCourseDaoComponent
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.columns.modules.{CoreRequiredModulesColumnOption, ModuleReportsColumnOption}
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.jobs.scheduling.ImportMembersJob
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.views.{ExcelView, JSONView}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

import scala.collection.JavaConverters._

object GenerateExamGridMappingParameters {
	final val selectCourse = "selectCourse"
	final val gridOptions = "gridOptions"
	final val coreRequiredModules = "coreRequiredModules"
	final val previewAndDownload = "previewAndDownload"
	final val export = "export"
}

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/generate"))
class GenerateExamGridController extends ExamsController
	with DepartmentScopedController with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent with AutowiringJobServiceComponent
	with AutowiringCourseDaoComponent with AutowiringModuleRegistrationServiceComponent {

	type SelectCourseCommand = Appliable[Seq[GenerateExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest with GenerateExamGridSelectCourseCommandState
	type GridOptionsCommand = Appliable[(Set[ExamGridColumnOption.Identifier], Seq[String])] with GenerateExamGridGridOptionsCommandRequest
	type CoreRequiredModulesCommand = Appliable[Seq[CoreRequiredModule]] with PopulateGenerateExamGridSetCoreRequiredModulesCommand

	override val departmentPermission: Permission = Permissions.Department.ExamGrids

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	private lazy val allExamGridsColumns: Seq[ExamGridColumnOption] = Wire.all[ExamGridColumnOption].sorted

	validatesSelf[SelfValidating]

	private def commonCrumbs(view: Mav, department: Department, academicYear: AcademicYear): Mav =
		view.crumbs(Breadcrumbs.Grids.Home, Breadcrumbs.Grids.Department(department, academicYear))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.exams.Grids.generate(department, year)): _*)

	@ModelAttribute("selectCourseCommand")
	def selectCourseCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateExamGridSelectCourseCommand(mandatory(department), mandatory(academicYear))

	@ModelAttribute("gridOptionsCommand")
	def gridOptionsCommand = GenerateExamGridGridOptionsCommand()

	@ModelAttribute("coreRequiredModulesCommand")
	def coreRequiredModulesCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateExamGridSetCoreRequiredModulesCommand(mandatory(department), mandatory(academicYear))

	@ModelAttribute("GenerateExamGridMappingParameters")
	def params = GenerateExamGridMappingParameters

	@ModelAttribute("coreRequiredModules")
	def coreRequiredModules(
		@PathVariable academicYear: AcademicYear,
		@RequestParam(value = "route", required = false) route: Route,
		@RequestParam(value = "yearOfStudy", required = false) yearOfStudy: JInteger
	) = {
		if (Option(route).nonEmpty && Option(yearOfStudy).nonEmpty) {
			moduleRegistrationService.findCoreRequiredModules(route, academicYear, yearOfStudy)
		} else {
			Seq()
		}
	}

	@RequestMapping(method = Array(GET, POST))
	def selectCourseRender(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		commonCrumbs(
			Mav("exams/grids/generate/selectCourse"),
			department,
			academicYear
		)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.selectCourse))
	def selectCourseSubmit(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		errors: Errors,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			selectCourseRender(selectCourseCommand, department, academicYear)
		} else {
			val students = selectCourseCommand.apply()
			if (students.isEmpty) {
				errors.reject("examGrid.noStudents")
				selectCourseRender(selectCourseCommand, department, academicYear)
			} else {
				val jobInstance = jobService.add(Some(user), ImportMembersJob(students.map(_.universityId)))
				if (gridOptionsCommand.predefinedColumnIdentifiers.isEmpty) {
					gridOptionsCommand.predefinedColumnIdentifiers.addAll(allExamGridsColumns.map(_.identifier).asJava)
				}
				gridOptionsRender(jobInstance.id, selectCourseCommand, department, academicYear)
			}
		}
	}

	private def gridOptionsRender(jobId: String, selectCourseCommand: SelectCourseCommand, department: Department, academicYear: AcademicYear): Mav = {
		commonCrumbs(Mav("exams/grids/generate/gridOption", "jobId" -> jobId), department, academicYear)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.gridOptions))
	def gridOptions(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		errors: Errors,
		@ModelAttribute("coreRequiredModulesCommand") coreRequiredModulesCommand: CoreRequiredModulesCommand,
		@ModelAttribute("coreRequiredModules") coreRequiredModules: Seq[CoreRequiredModule],
		@RequestParam jobId: String,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			gridOptionsRender(jobId, selectCourseCommand, department, academicYear)
		} else {
			val columnIDs = gridOptionsCommand.apply()
			if (columnIDs._1.contains(new CoreRequiredModulesColumnOption().identifier) || columnIDs._1.contains(new ModuleReportsColumnOption().identifier)) {
				coreRequiredModulesCommand.populate()
				coreRequiredModulesRender(jobId, selectCourseCommand, department, academicYear)
			} else {
				checkJobProgress(jobId, selectCourseCommand, gridOptionsCommand, coreRequiredModules, department, academicYear)
			}
		}
	}

	private def coreRequiredModulesRender(jobId: String, selectCourseCommand: SelectCourseCommand, department: Department, academicYear: AcademicYear): Mav = {
		commonCrumbs(
			Mav("exams/grids/generate/coreRequiredModules", "jobId" -> jobId),
			department,
			academicYear
		)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.coreRequiredModules))
	def coreRequiredModules(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@Valid @ModelAttribute("coreRequiredModulesCommand") coreRequiredModulesCommand: CoreRequiredModulesCommand,
		errors: Errors,
		@RequestParam jobId: String,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			coreRequiredModulesRender(jobId, selectCourseCommand, department, academicYear)
		} else {
			val coreRequiredModules = coreRequiredModulesCommand.apply()
			checkJobProgress(jobId, selectCourseCommand, gridOptionsCommand, coreRequiredModules, department, academicYear)
		}
	}

	private def checkJobProgress(
		jobId: String,
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		coreRequiredModules: Seq[CoreRequiredModule],
		department: Department,
		academicYear: AcademicYear
	): Mav = {
		val jobInstance = jobService.getInstance(jobId)
		if (jobInstance.isDefined && !jobInstance.get.finished) {
			val studentLastImportDates = selectCourseCommand.apply().map(_.studentCourseYearDetails.get.studentCourseDetails.student).distinct.map(s =>
				(s.fullName, Option(s.lastImportDate).getOrElse(new DateTime(0)))
			).sortBy(_._2)
			commonCrumbs(
				Mav("exams/grids/generate/jobProgress",
					"jobId" -> jobId,
					"jobProgress" -> jobInstance.get.progress,
					"jobStatus" -> jobInstance.get.status,
					"studentLastImportDates" -> studentLastImportDates
				),
				department,
				academicYear
			)
		} else {
			previewAndDownloadRender(selectCourseCommand, gridOptionsCommand, coreRequiredModules, department, academicYear)
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
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		gridOptionsCommandErrors: Errors,
		@ModelAttribute("coreRequiredModules") coreRequiredModules: Seq[CoreRequiredModule],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (selectCourseCommandErrors.hasErrors || gridOptionsCommandErrors.hasErrors) {
			throw new IllegalArgumentException
		}
		previewAndDownloadRender(selectCourseCommand, gridOptionsCommand, coreRequiredModules, department, academicYear)
	}

	private def previewAndDownloadRender(
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		coreRequiredModules: Seq[CoreRequiredModule],
		department: Department,
		academicYear: AcademicYear
	): Mav = {
		val (entities, columns, weightings) = gridData(selectCourseCommand, gridOptionsCommand, coreRequiredModules)
		val columnValues = columns.map(_.render)
		val categories = columns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)

		commonCrumbs(
			Mav("exams/grids/generate/preview",
				"columns" -> columns,
				"columnValues" -> columnValues,
				"categories" -> categories,
				"scyds" -> entities,
				"generatedDate" -> DateTime.now,
				"weightings" -> weightings
			),
			department,
			academicYear
		)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.export))
	def export(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		gridOptionsCommandErrors: Errors,
		@ModelAttribute("coreRequiredModules") coreRequiredModules: Seq[CoreRequiredModule],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): View = {
		if (selectCourseCommandErrors.hasErrors || gridOptionsCommandErrors.hasErrors) {
			throw new IllegalArgumentException
		}
		val (entities, columns, weightings) = gridData(selectCourseCommand, gridOptionsCommand, coreRequiredModules)

		new ExcelView(
			s"Exam grid for ${department.name} ${selectCourseCommand.course.code} ${selectCourseCommand.route.code.toUpperCase} ${academicYear.toString.replace("/","-")}.xlsx",
			GenerateExamGridExporter(
				department,
				academicYear,
				selectCourseCommand.course,
				selectCourseCommand.route,
				selectCourseCommand.yearOfStudy,
				weightings,
				entities,
				columns
			)
		)
	}

	private def gridData(
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		coreRequiredModules: Seq[CoreRequiredModule]
	): (Seq[GenerateExamGridEntity], Seq[ExamGridColumn], Seq[CourseYearWeighting]) = {
		val entities = selectCourseCommand.apply().sortBy(_.studentCourseYearDetails.get.studentCourseDetails.scjCode)

		val gridOptions = gridOptionsCommand.apply()
		val predefinedColumnIDs = gridOptions._1
		val customColumnTitles = gridOptions._2
		val normalLoad = ModuleRegistrationService.DefaultNormalLoad // TODO Check the URRs for the normal load
		val state = ExamGridColumnState(
			entities = entities,
			overcatSubsets = entities.filter(_.cats > ModuleRegistrationService.DefaultNormalLoad).map(entity => entity ->
				moduleRegistrationService.overcattedModuleSubsets(entity, entity.markOverrides.getOrElse(Map()), normalLoad)
			).toMap,
			coreRequiredModules = coreRequiredModules.map(_.module),
			normalLoad = normalLoad,
			routeRules = Seq(), // TODO Fetch the URRs
			yearOfStudy = selectCourseCommand.yearOfStudy
		)

		val predefinedColumns = allExamGridsColumns.filter(c => c.mandatory || predefinedColumnIDs.contains(c.identifier)).flatMap(_.getColumns(state))
		val customColumns = customColumnTitles.flatMap(BlankColumnOption.getColumn)
		val columns = predefinedColumns ++ customColumns

		val weightings = (1 to FilterStudentsOrRelationships.MaxYearsOfStudy).flatMap(year =>
			courseDao.getCourseYearWeighting(selectCourseCommand.course.code, selectCourseCommand.academicYear, year)
		).sorted

		(entities, columns, weightings)
	}

}
