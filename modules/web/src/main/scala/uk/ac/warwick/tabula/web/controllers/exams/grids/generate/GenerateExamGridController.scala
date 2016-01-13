package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import org.springframework.web.servlet.View
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.exams.grids._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Module, Department, StudentCourseYearDetails}
import uk.ac.warwick.tabula.exams.grids.columns.modules.{CoreRequiredModulesColumnOption, ModulesColumnOption}
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, BlankColumnOption, HasExamGridColumnCategory, ExamGridColumnOption}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.views.{ExcelView, JSONView}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

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
	with AutowiringMaintenanceModeServiceComponent with AutowiringJobServiceComponent {

	type SelectCourseCommand = Appliable[Seq[GenerateExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest with GenerateExamGridSelectCourseCommandState
	type GridOptionsCommand = Appliable[(Set[ExamGridColumnOption.Identifier], Seq[String])]

	override val departmentPermission: Permission = Permissions.Department.ExamGrids

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	validatesSelf[SelfValidating]

	private def commonCrumbs(view: Mav, department: Department, academicYear: AcademicYear): Mav =
		view.crumbs(Breadcrumbs.Grids.Home, Breadcrumbs.Grids.Department(department, academicYear))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.exams.Grids.generate(department, year)): _*)

	@ModelAttribute("selectCourseCommand")
	def selectCourseCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateExamGridSelectCourseCommand(mandatory(department), mandatory(academicYear))

	@ModelAttribute("GenerateExamGridMappingParameters")
	def params = GenerateExamGridMappingParameters

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
				val jobId = jobService.addSchedulerJob("import-members", Map("members" -> students.map(_.universityId)), user.apparentUser)
				gridOptionsRender(jobId, selectCourseCommand, department, academicYear)
			}
		}
	}

	private def gridOptionsRender(jobId: String, selectCourseCommand: SelectCourseCommand, department: Department, academicYear: AcademicYear): Mav = {
		val coreRequiredModules = selectCourseCommand.department.getCoreRequiredModules(
			selectCourseCommand.academicYear,
			selectCourseCommand.course,
			selectCourseCommand.route,
			selectCourseCommand.yearOfStudy
		)
		commonCrumbs(
			Mav("exams/grids/generate/gridOption",
				"jobId" -> jobId,
				"coreRequiredModulesRequired" -> coreRequiredModules.isEmpty
			),
			department,
			academicYear
		)
	}

	@ModelAttribute("gridOptionsCommand")
	def gridOptionsCommand = GenerateExamGridGridOptionsCommand()

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.gridOptions))
	def gridOptions(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		errors: Errors,
		@RequestParam jobId: String,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			gridOptionsRender(jobId, selectCourseCommand, department, academicYear)
		} else {
			val scyds = selectCourseCommand.apply()
			val columnIDs = gridOptionsCommand.apply()
			val coreRequiredModules = selectCourseCommand.department.getCoreRequiredModules(
				selectCourseCommand.academicYear,
				selectCourseCommand.course,
				selectCourseCommand.route,
				selectCourseCommand.yearOfStudy
			)
			if (columnIDs._1.contains(new CoreRequiredModulesColumnOption().identifier) && coreRequiredModules.isEmpty) {
				commonCrumbs(
					Mav("exams/grids/generate/coreRequiredModules",
						"jobId" -> jobId,
						"modules" -> scyds.flatMap(_.moduleRegistrations.map(_.module)).distinct.sortBy(_.code)
					),
					department,
					academicYear
				)
			} else {
				checkJobProgress(jobId, selectCourseCommand, gridOptionsCommand, department, academicYear)
			}
		}
	}

	@ModelAttribute("coreRequiredModulesCommand")
	def coreRequiredModulesCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateExamGridSetCoreRequiredModulesCommand(mandatory(department), mandatory(academicYear))

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.coreRequiredModules))
	def coreRequiredModules(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@ModelAttribute("coreRequiredModulesCommand") coreRequiredModulesCommand: Appliable[Seq[Module]],
		@RequestParam jobId: String,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		coreRequiredModulesCommand.apply()
			val columnIDs = gridOptionsCommand.apply()
			val coreRequiredModules = selectCourseCommand.department.getCoreRequiredModules(
				selectCourseCommand.academicYear,
				selectCourseCommand.course,
				selectCourseCommand.route,
				selectCourseCommand.yearOfStudy
			)
			if (columnIDs._1.contains(new CoreRequiredModulesColumnOption().identifier) && coreRequiredModules.isEmpty) {
				commonCrumbs(
					Mav("exams/grids/generate/coreRequiredModules", "jobId" -> jobId),
					department,
					academicYear
				)
			} else {
				checkJobProgress(jobId, selectCourseCommand, gridOptionsCommand, department, academicYear)
			}
	}

	private def checkJobProgress(
		jobId: String,
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
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
			previewAndDownloadRender(selectCourseCommand, gridOptionsCommand, department, academicYear)
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
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (selectCourseCommandErrors.hasErrors || gridOptionsCommandErrors.hasErrors) {
			throw new IllegalArgumentException
		}
		previewAndDownloadRender(selectCourseCommand, gridOptionsCommand, department, academicYear)
	}

	private def previewAndDownloadRender(
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		department: Department,
		academicYear: AcademicYear
	): Mav = {
		val (scyds, columns) = gridData(selectCourseCommand, gridOptionsCommand)
		val columnValues = columns.map(_.render)
		val categories = columns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)

		commonCrumbs(
			Mav("exams/grids/generate/preview",
				"columns" -> columns,
				"columnValues" -> columnValues,
				"categories" -> categories,
				"scyds" -> scyds,
				"generatedDate" -> DateTime.now
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
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): View = {
		if (selectCourseCommandErrors.hasErrors || gridOptionsCommandErrors.hasErrors) {
			throw new IllegalArgumentException
		}
		val (scyds, columns) = gridData(selectCourseCommand, gridOptionsCommand)

		new ExcelView(
			s"Exam grid for ${department.name} ${selectCourseCommand.course.code} ${selectCourseCommand.route.code.toUpperCase} ${academicYear.toString.replace("/","-")}.xlsx",
			GenerateExamGridExporter(scyds, columns, academicYear)
		)
	}

	private def gridData(
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand
	): (Seq[GenerateExamGridEntity], Seq[ExamGridColumn]) = {
		val scyds = selectCourseCommand.apply().sortBy(_.studentCourseYearDetails.get.studentCourseDetails.scjCode)

		val gridOptions = gridOptionsCommand.apply()
		val predefinedColumnIDs = gridOptions._1
		val customColumnTitles = gridOptions._2

		val allExamGridsColumns: Seq[ExamGridColumnOption] = Wire.all[ExamGridColumnOption].sorted
		val predefinedColumns = allExamGridsColumns.filter(c => predefinedColumnIDs.contains(c.identifier)).flatMap{
			case coreRequiredColumn: ModulesColumnOption => coreRequiredColumn.getColumns(
				selectCourseCommand.department.getCoreRequiredModules(selectCourseCommand.academicYear, selectCourseCommand.course, selectCourseCommand.route, selectCourseCommand.yearOfStudy).getOrElse(Seq()),
				scyds
			)
			case column => column.getColumns(scyds)
		}
		val customColumns = customColumnTitles.flatMap(BlankColumnOption.getColumn)
		val columns = predefinedColumns ++ customColumns

		(scyds, columns)
	}

}
