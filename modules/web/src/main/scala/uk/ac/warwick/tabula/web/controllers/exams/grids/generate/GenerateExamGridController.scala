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
import uk.ac.warwick.tabula.data.model.{Department, StudentCourseYearDetails}
import uk.ac.warwick.tabula.exams.grids.columns.{HasExamGridColumnCategory, ExamGridColumnOption}
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
	final val previewAndDownload = "previewAndDownload"
	final val export = "export"
}

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/generate"))
class GenerateExamGridController extends ExamsController
	with DepartmentScopedController with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent with AutowiringJobServiceComponent {

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
		@ModelAttribute("selectCourseCommand") selectCourseCommand: Appliable[Seq[StudentCourseYearDetails]],
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
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: Appliable[Seq[StudentCourseYearDetails]],
		errors: Errors,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: Appliable[Set[ExamGridColumnOption.Identifier]],
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
				// TODO Uncomment this
				val jobId = jobService.addSchedulerJob("import-members", Map("members" -> students.map(_.studentCourseDetails.student.universityId)), user.apparentUser)
				gridOptionsRender(jobId, department, academicYear)
				//gridOptionsRender("4254f2a5-2f75-49da-b3f3-6ed3f531d077", department, academicYear)
			}
		}
	}

	private def gridOptionsRender(jobId: String, department: Department, academicYear: AcademicYear): Mav = {
		commonCrumbs(
			Mav("exams/grids/generate/gridOption",
			"jobId" -> jobId
			),
			department,
			academicYear
		)
	}

	@ModelAttribute("gridOptionsCommand")
	def gridOptionsCommand = GenerateExamGridGridOptionsCommand()

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.gridOptions))
	def gridOptions(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: Appliable[Seq[StudentCourseYearDetails]],
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: Appliable[Set[ExamGridColumnOption.Identifier]],
		errors: Errors,
		@RequestParam jobId: String,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			gridOptionsRender(jobId, department, academicYear)
		} else {
			val columnIDs = gridOptionsCommand.apply()
			val jobInstance = jobService.getInstance(jobId)
			if (jobInstance.isDefined && !jobInstance.get.finished) {
				val scyds = selectCourseCommand.apply()
				val studentLastImportDates = scyds.map(_.studentCourseDetails.student).distinct.map(s =>
					(s.fullName, Option(s.lastImportDate).getOrElse(new DateTime(0)))
				).sortBy(_._2)
				commonCrumbs(
					Mav("exams/grids/generate/jobProgress",
						"jobId" -> jobId,
						"jobProgress" -> jobInstance.get.progress,
						"jobStatus" -> jobInstance.get.status,
						"columnIDs" -> columnIDs,
						"studentLastImportDates" -> studentLastImportDates
					),
					department,
					academicYear
				)
			} else {
				previewAndDownloadRender(selectCourseCommand, gridOptionsCommand, department, academicYear)
			}
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
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: Appliable[Seq[StudentCourseYearDetails]],
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: Appliable[Set[ExamGridColumnOption.Identifier]],
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
		selectCourseCommand: Appliable[Seq[StudentCourseYearDetails]],
		gridOptionsCommand: Appliable[Set[ExamGridColumnOption.Identifier]],
		department: Department,
		academicYear: AcademicYear
	): Mav = {
		val scyds = selectCourseCommand.apply().sortBy(_.studentCourseDetails.scjCode)
		val columnIDs = gridOptionsCommand.apply()
		val allExamGridsColumns: Seq[ExamGridColumnOption] = Wire.all[ExamGridColumnOption].sorted
		val columns = allExamGridsColumns.filter(c => columnIDs.contains(c.identifier)).flatMap(_.getColumns(scyds))
		val columnValues = columns.map(_.render)
		val categories = columns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)
		commonCrumbs(
			Mav("exams/grids/generate/preview",
				"columns" -> columns,
				"columnValues" -> columnValues,
				"categories" -> categories,
				"scyds" -> scyds,
				"columnIDs" -> columnIDs,
				"generatedDate" -> DateTime.now
			),
			department,
			academicYear
		)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.export))
	def export(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: Appliable[Seq[StudentCourseYearDetails]] with GenerateExamGridSelectCourseCommandRequest,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: Appliable[Set[ExamGridColumnOption.Identifier]],
		gridOptionsCommandErrors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): View = {
		if (selectCourseCommandErrors.hasErrors || gridOptionsCommandErrors.hasErrors) {
			throw new IllegalArgumentException
		}
		val scyds = selectCourseCommand.apply().sortBy(_.studentCourseDetails.scjCode)
		val columnIDs = gridOptionsCommand.apply()
		val allExamGridsColumns: Seq[ExamGridColumnOption] = Wire.all[ExamGridColumnOption].sorted
		val columns = allExamGridsColumns.filter(c => columnIDs.contains(c.identifier)).map(_.getColumns(scyds))

		new ExcelView(
			s"Exam grid for ${department.name} ${selectCourseCommand.course.code} ${selectCourseCommand.route.code.toUpperCase} ${academicYear.toString.replace("/","-")}.xlsx",
			GenerateExamGridExporter(scyds, columns, academicYear)
		)
	}

}
