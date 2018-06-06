package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import javax.validation.Valid
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.util.MultiValueMap
import org.springframework.validation.{BindException, Errors}
import org.springframework.web.bind.annotation._
import org.springframework.web.util.UriComponentsBuilder
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids._
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.columns.modules.{CoreRequiredModulesColumnOption, ModuleExamGridColumn, ModuleReportsColumn, ModuleReportsColumnOption}
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumnValueType, _}
import uk.ac.warwick.tabula.exams.web.Routes.Grids
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.jobs.scheduling.ImportMembersJob
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.NormalLoadLookup
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap

object GenerateExamGridMappingParameters {
	final val selectCourse = "selectCourse"
	final val usePreviousSettings = "usePreviousSettings"
	final val gridOptions = "gridOptions"
	final val coreRequiredModules = "coreRequiredModules"
	final val previewAndDownload = "previewAndDownload"
	final val excel = "excel"
	final val excelNoMergedCells = "excelNoMergedCells"
	final val marksRecord = "marksRecord"
	final val marksRecordConfidential = "marksRecordConfidential"
	final val passList = "passList"
	final val passListConfidential = "passListConfidential"
	final val transcript = "transcript"
	final val transcriptConfidential = "transcriptConfidential"
}

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/generate"))
class GenerateExamGridController extends ExamsController
	with DepartmentScopedController with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent with AutowiringJobServiceComponent
	with AutowiringCourseAndRouteServiceComponent with AutowiringModuleRegistrationServiceComponent
	with ExamGridDocumentsController
	with TaskBenchmarking {

	type SelectCourseCommand = Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest with GenerateExamGridSelectCourseCommandState
	type GridOptionsCommand = Appliable[(Seq[ExamGridColumnOption], Seq[String])] with GenerateExamGridGridOptionsCommandRequest with PopulateOnForm
	type CoreRequiredModulesCommand = Appliable[Map[Route, Seq[CoreRequiredModule]]] with PopulateGenerateExamGridSetCoreRequiredModulesCommand
	type CheckOvercatCommand = Appliable[GenerateExamGridCheckAndApplyOvercatCommand.Result] with GenerateExamGridCheckAndApplyOvercatCommandState with SelfValidating

	override val departmentPermission: Permission = Permissions.Department.ExamGrids

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	validatesSelf[SelfValidating]

	private def commonCrumbs(view: Mav, department: Department, academicYear: AcademicYear): Mav =
		view.crumbs(Breadcrumbs.Grids.Home, Breadcrumbs.Grids.Department(department, academicYear))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Grids.generate(department, year)): _*)

	@ModelAttribute("selectCourseCommand")
	def selectCourseCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateExamGridSelectCourseCommand(mandatory(department), mandatory(academicYear), permitRoutesFromRootDepartment = securityService.can(user, departmentPermission, department.rootDepartment))

	@ModelAttribute("gridOptionsCommand")
	def gridOptionsCommand(@PathVariable department: Department) = {
		if (maintenanceModeService.enabled)
			GenerateExamGridGridOptionsCommand.applyReadOnly(mandatory(department))
		else
			GenerateExamGridGridOptionsCommand(mandatory(department))
	}

	@ModelAttribute("coreRequiredModulesCommand")
	def coreRequiredModulesCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateExamGridSetCoreRequiredModulesCommand(mandatory(department), mandatory(academicYear))

	@ModelAttribute("checkOvercatCommand")
	def checkOvercatCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateExamGridCheckAndApplyOvercatCommand(department, academicYear, user)

	@ModelAttribute("GenerateExamGridMappingParameters")
	def params = GenerateExamGridMappingParameters

	@ModelAttribute("gridOptionsQueryString")
	def gridOptionsQueryString(@RequestParam params: MultiValueMap[String, String]): String = {
		params.keySet.asScala.filter(_.startsWith("modules[")).foreach(params.remove)
		UriComponentsBuilder.newInstance().queryParams(params).build().getQuery
	}

	@ModelAttribute("coreRequiredModuleLookup")
	def coreRequiredModuleLookup(
		@PathVariable academicYear: AcademicYear,
		@RequestParam(value = "yearOfStudy", required = false) yearOfStudy: JInteger,
		@RequestParam(value = "levelCode", required = false) levelCode: String
	): CoreRequiredModuleLookup = {
		if (Option(yearOfStudy).nonEmpty) {
			new CoreRequiredModuleLookupImpl(academicYear, yearOfStudy, moduleRegistrationService)
		} else if (levelCode != null){
			new CoreRequiredModuleLookupImpl(academicYear, Level.toYearOfStudy(levelCode), moduleRegistrationService)
		} else {
			null
		}
	}

	@ModelAttribute("ExamGridColumnValueType")
	def examGridColumnValueType = ExamGridColumnValueType

	@GetMapping
	def selectCourseRender(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		gridOptionsCommand.populate()
		commonCrumbs(
			Mav("exams/grids/generate/selectCourse"),
			department,
			academicYear
		)
	}

	@PostMapping(params = Array(GenerateExamGridMappingParameters.selectCourse))
	def selectCourseSubmit(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		errors: Errors,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@ModelAttribute("checkOvercatCommand") checkOvercatCommand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam allRequestParams: MultiValueMap[String, String]
	): Mav = {
		selectCourseSubmit(
			selectCourseCommand,
			errors,
			gridOptionsCommand,
			checkOvercatCommand,
			coreRequiredModuleLookup,
			department,
			academicYear,
			usePreviousSettings = false,
			allRequestParams
		)
	}

	@PostMapping(params = Array(GenerateExamGridMappingParameters.usePreviousSettings))
	def selectCourseSubmitWithPreviousSettings(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		errors: Errors,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@ModelAttribute("checkOvercatCommand") checkOvercatCommand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam allRequestParams: MultiValueMap[String, String]
	): Mav = {
		selectCourseSubmit(
			selectCourseCommand,
			errors,
			gridOptionsCommand,
			checkOvercatCommand,
			coreRequiredModuleLookup,
			department,
			academicYear,
			usePreviousSettings = true,
			allRequestParams
		)
	}

	private def selectCourseSubmit(
		selectCourseCommand: SelectCourseCommand,
		errors: Errors,
		gridOptionsCommand: GridOptionsCommand,
		checkOvercatCommand: CheckOvercatCommand,
		coreRequiredModuleLookup: CoreRequiredModuleLookup,
		department: Department,
		academicYear: AcademicYear,
		usePreviousSettings: Boolean,
		allRequestParams: MultiValueMap[String, String]
	): Mav = {
		if (errors.hasErrors) {
			selectCourseRender(selectCourseCommand, gridOptionsCommand, department, academicYear)
		} else {
			val students = selectCourseCommand.apply()
			if (students.isEmpty) {
				errors.reject("examGrid.noStudents")
				selectCourseRender(selectCourseCommand, gridOptionsCommand, department, academicYear)
			} else {
				if (!maintenanceModeService.enabled) {
					stopOngoingImportForStudents(students)

					val jobInstance = jobService.add(Some(user), ImportMembersJob(students.map(_.universityId)))

					allRequestParams.set("jobId", jobInstance.id)
				}
				if (usePreviousSettings) {
					redirectToAndClearModel(Grids.jobProgress(department,academicYear), allRequestParams)
				} else {
					redirectToAndClearModel(Grids.options(department, academicYear), allRequestParams)
				}
			}
		}
	}

	@GetMapping(path = Array("/options"))
	def gridOptionsForm(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@ModelAttribute("coreRequiredModulesCommand") coreRequiredModulesCommand: CoreRequiredModulesCommand,
		@ModelAttribute("checkOvercatCommand") checkOvercatCommand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@RequestParam(required = false) jobId: String,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
	): Mav = {
		gridOptionsRender(jobId, department, academicYear)
	}

	@PostMapping(path = Array("/options"))
	def gridOptions(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		errors: Errors,
		@ModelAttribute("coreRequiredModulesCommand") coreRequiredModulesCommand: CoreRequiredModulesCommand,
		@ModelAttribute("checkOvercatCommand") checkOvercatCommand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@RequestParam(required = false) jobId: String,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam allRequestParams: MultiValueMap[String, String]
	): Mav = {
		if (errors.hasErrors) {
			gridOptionsRender(jobId, department, academicYear)
		} else {
			val columnIDs = gridOptionsCommand.apply()._1.map(_.identifier)

			if (!maintenanceModeService.enabled && columnIDs.contains(new CoreRequiredModulesColumnOption().identifier) || columnIDs.contains(new ModuleReportsColumnOption().identifier)) {
				redirectToAndClearModel(Grids.coreRequired(department,academicYear), allRequestParams)
			} else {
				redirectToAndClearModel(Grids.jobProgress(department,academicYear), allRequestParams)
			}
		}
	}

	private def gridOptionsRender(jobId: String, department: Department, academicYear: AcademicYear): Mav = {
		commonCrumbs(Mav("exams/grids/generate/gridOption", "jobId" -> jobId), department, academicYear)
	}

	@GetMapping(path = Array("/corerequired"))
	def coreRequiredModulesForm(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@ModelAttribute("coreRequiredModulesCommand") coreRequiredModulesCommand: CoreRequiredModulesCommand,
		@ModelAttribute("checkOvercatCommand") checkOvercatCommand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@RequestParam(required = false) jobId: String,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		coreRequiredModulesCommand.populate()
		coreRequiredModulesRender(jobId, department, academicYear)
	}

	@PostMapping(path = Array("/corerequired"))
	def coreRequiredModules(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@Valid @ModelAttribute("coreRequiredModulesCommand") coreRequiredModulesCommand: CoreRequiredModulesCommand,
		errors: Errors,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@ModelAttribute("checkOvercatCommand") checkOvercatCommand: CheckOvercatCommand,
		@RequestParam(required = false) jobId: String,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam allRequestParams: MultiValueMap[String, String]
	): Mav = {
		if (errors.hasErrors) {
			coreRequiredModulesRender(jobId, department, academicYear)
		} else {
			coreRequiredModulesCommand.apply()
			redirectToAndClearModel(Grids.jobProgress(department, academicYear), allRequestParams)
		}
	}

	private def coreRequiredModulesRender(jobId: String, department: Department, academicYear: AcademicYear): Mav = {
		commonCrumbs(
			Mav("exams/grids/generate/coreRequiredModules", "jobId" -> jobId),
			department,
			academicYear
		)
	}

	@GetMapping(path = Array("/import"))
	def checkJobProgress(
		@RequestParam(required = false) jobId: String,
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@ModelAttribute("checkOvercatCommand") checkOvercatCommand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam allRequestParams: MultiValueMap[String, String]
	): Mav = {
		val jobInstance = Option(jobId).flatMap(jobService.getInstance)
		if (jobInstance.isDefined && !jobInstance.get.finished) {
			val studentLastImportDates = selectCourseCommand.apply().map(e =>
				(Seq(e.firstName, e.lastName).mkString(" "), e.lastImportDate.getOrElse(new DateTime(0)))
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
			redirectToAndClearModel(Grids.preview(department, academicYear), allRequestParams)
		}
	}

	@PostMapping(path = Array("/progress"))
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

	@PostMapping(path = Array("/import/skip"))
	def skipImportAndGenerateGrid(
		@RequestParam jobId: String,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam allRequestParams: MultiValueMap[String, String]
	): Mav = {
		jobService.getInstance(jobId)
			.filter(_.jobType == ImportMembersJob.identifier)
			.foreach(jobService.kill)

		redirectToAndClearModel(Grids.preview(department, academicYear), allRequestParams)
	}

	@GetMapping(path = Array("/preview"))
	def previewAndDownload(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		gridOptionsCommandErrors: Errors,
		@ModelAttribute("checkOvercatCommand") checkOvercatCommand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (selectCourseCommandErrors.hasErrors || gridOptionsCommandErrors.hasErrors) {
			throw new IllegalArgumentException
		}

		val GridData(entities, studentInformationColumns, perYearColumns, summaryColumns, weightings, normalLoadLookup, routeRules) = benchmarkTask("GridData") {
			checkAndApplyOvercatAndGetGridData(
				selectCourseCommand,
				gridOptionsCommand,
				checkOvercatCommand,
				coreRequiredModuleLookup
			)
		}

		val oldestImport = benchmarkTask("oldestImport") { entities.flatMap(_.lastImportDate).reduceOption((a, b) => if(a.isBefore(b)) a else b) }
		val chosenYearColumnValues = benchmarkTask("chosenYearColumnValues") { Seq(studentInformationColumns, summaryColumns).flatten.map(c => c -> c.values).toMap }
		val chosenYearColumnCategories = benchmarkTask("chosenYearColumnCategories") { summaryColumns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category) }
		val perYearColumnValues = benchmarkTask("perYearColumnValues") { perYearColumns.values.flatten.toSeq.map(c => c -> c.values).toMap }
		val perYearColumnCategories = benchmarkTask("perYearColumnCategories") { perYearColumns.mapValues(_.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)) }

		val shortFormLayoutData = if (!gridOptionsCommand.showFullLayout) {
			val perYearModuleMarkColumns = benchmarkTask("perYearModuleMarkColumns"){ perYearColumns.map{ case (year, columns) => year -> columns.collect{ case marks: ModuleExamGridColumn => marks}} }
			val perYearModuleReportColumns  = benchmarkTask("perYearModuleReportColumns"){ perYearColumns.map{ case (year, columns) => year -> columns.collect{ case marks: ModuleReportsColumn => marks}} }

			val maxYearColumnSize =  benchmarkTask("maxYearColumnSize") { perYearModuleMarkColumns.map{ case (year, columns) =>
				val maxModuleColumns = (entities.map(entity => columns.count(c => !c.isEmpty(entity, year))) ++ Seq(1)).max
				year -> maxModuleColumns
			} }

			Map(
				"maxYearColumnSize" -> maxYearColumnSize,
				"perYearModuleMarkColumns" -> perYearModuleMarkColumns,
				"perYearModuleReportColumns" -> perYearModuleReportColumns
			)
		} else { Map[String, Object]() }

		val mavObjects = Map(
			"oldestImport" -> oldestImport,
			"studentInformationColumns" -> studentInformationColumns,
			"perYearColumns" -> perYearColumns,
			"summaryColumns" -> summaryColumns,
			"chosenYearColumnValues" -> chosenYearColumnValues,
			"perYearColumnValues" -> perYearColumnValues,
			"chosenYearColumnCategories" -> chosenYearColumnCategories,
			"perYearColumnCategories" -> perYearColumnCategories,
			"entities" -> entities,
			"generatedDate" -> DateTime.now,
			"weightings" -> weightings,
			"normalLoadLookup" -> normalLoadLookup,
			"routeRules" -> routeRules
		) ++ shortFormLayoutData

		commonCrumbs(
			Mav("exams/grids/generate/preview", mavObjects).bodyClasses("grid-preview"),
			department,
			academicYear
		)
	}

	protected case class GridData(
		entities: Seq[ExamGridEntity],
		studentInformationColumns: Seq[ChosenYearExamGridColumn],
		perYearColumns: Map[YearOfStudy, Seq[PerYearExamGridColumn]],
		summaryColumns: Seq[ChosenYearExamGridColumn],
		weightings: Map[Course, Seq[CourseYearWeighting]],
		normalLoadLookup: NormalLoadLookup,
		routeRulesLookup: UpstreamRouteRuleLookup
	)

	protected def checkAndApplyOvercatAndGetGridData(
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		checkOvercatCmd: CheckOvercatCommand,
		coreRequiredModuleLookup: CoreRequiredModuleLookup
	): GridData = {

		checkOvercatCmd.selectCourseCommand = selectCourseCommand
		val checkOvercatCommandErrors = new BindException(selectCourseCommand, "checkOvercatCommand")
		checkOvercatCmd.validate(checkOvercatCommandErrors)

		val entities = {
			if (checkOvercatCommandErrors.hasErrors || maintenanceModeService.enabled) {
				checkOvercatCmd.entities
			} else {
				checkOvercatCmd.apply().entities
			}
		}

		val normalLoadLookup = checkOvercatCmd.normalLoadLookup
		val routeRulesLookup = checkOvercatCmd.routeRulesLookup

		val (predefinedColumnOptions, customColumnTitles) = gridOptionsCommand.apply()

		val overcatSubsets = entities
			.flatMap(entity => {entity.validYears.get(selectCourseCommand.studyYearByLevelOrBlock).map((entity, _))})
			.map{ case (entity, entityYear) =>
				entityYear -> moduleRegistrationService.overcattedModuleSubsets(
					entityYear,
					entityYear.markOverrides.getOrElse(Map()),
					normalLoadLookup(entityYear.route),
					routeRulesLookup(entityYear.route, entityYear.level)
				)
			}.toMap

		val coreRequiredModulesColumnSelected = predefinedColumnOptions.exists(_.isInstanceOf[CoreRequiredModulesColumnOption])

		val state = ExamGridColumnState(
			entities = entities,
			overcatSubsets = overcatSubsets,
			coreRequiredModuleLookup = if (coreRequiredModulesColumnSelected) coreRequiredModuleLookup else NullCoreRequiredModuleLookup,
			normalLoadLookup = normalLoadLookup,
			routeRulesLookup = routeRulesLookup,
			academicYear = selectCourseCommand.academicYear,
			yearOfStudy = selectCourseCommand.studyYearByLevelOrBlock,
			department = selectCourseCommand.department,
			nameToShow = gridOptionsCommand.nameToShow,
			showComponentMarks = gridOptionsCommand.showComponentMarks,
			showZeroWeightedComponents = gridOptionsCommand.showZeroWeightedComponents,
			showComponentSequence = gridOptionsCommand.showComponentSequence,
			showModuleNames = gridOptionsCommand.moduleNameToShow,
			calculateYearMarks = gridOptionsCommand.calculateYearMarks,
			isLevelGrid = selectCourseCommand.isLevelGrid
		)

		val studentInformationColumns = predefinedColumnOptions.collect { case c: StudentExamGridColumnOption => c }.flatMap(_.getColumns(state))
		val summaryColumns = predefinedColumnOptions.collect { case c: ChosenYearExamGridColumnOption => c }.flatMap(_.getColumns(state)) ++
			customColumnTitles.flatMap(BlankColumnOption.getColumn)
		val selectedYears = selectCourseCommand.courseYearsToShow.asScala.map(_.stripPrefix("Year").toInt).toSeq
		val perYearColumns = predefinedColumnOptions.collect { case c: PerYearExamGridColumnOption => c }
			.flatMap(_.getColumns(state).toSeq)
			.filter { case (year, _) => selectedYears.contains(year) }
			.groupBy { case (year, _) => year}
			.mapValues(_.flatMap { case (_, columns) => columns })

		val weightings = ListMap(selectCourseCommand.courses.asScala.map(course => {
			course -> (1 to FilterStudentsOrRelationships.MaxYearsOfStudy).flatMap(year =>
				courseAndRouteService.getCourseYearWeighting(course.code, selectCourseCommand.academicYear, year)
			).sorted
		}).sortBy{case (course, _) => course.code} :_*)

		GenerateExamGridAuditCommand(selectCourseCommand).apply()

		GridData(entities, studentInformationColumns, perYearColumns, summaryColumns, weightings, normalLoadLookup, routeRulesLookup)
	}

	private def stopOngoingImportForStudents(students: Seq[ExamGridEntity]): Unit = {
		val members = students.map(_.universityId).toSet

		jobService.jobDao.listRunningJobs
			.filter(_.jobType == ImportMembersJob.identifier)
			.filter(_.getStrings(ImportMembersJob.MembersKey).toSet == members)
			.foreach(jobService.kill)
	}

	private def redirectToAndClearModel(path: String, params: MultiValueMap[String, String]): Mav = {
		params.set("clearModel", "true")
		params.keySet.asScala.filter(_.startsWith("modules[")).foreach(params.remove)
		
		val uri = UriComponentsBuilder.fromPath(path).queryParams(params).toUriString
		RedirectForce(uri)
	}
}
