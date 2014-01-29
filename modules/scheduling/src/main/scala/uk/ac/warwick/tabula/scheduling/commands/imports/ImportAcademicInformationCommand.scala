package uk.ac.warwick.tabula.scheduling.commands.imports

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.scheduling.services._
import uk.ac.warwick.tabula.system.permissions._
import uk.ac.warwick.tabula.services._
import ImportAcademicInformationCommand._

object ImportAcademicInformationCommand {
	def apply() =
		new ImportAcademicInformationCommandInternal
			with ComposableCommand[ImportAcademicInformationResults]
			with ImportDepartments
			with ImportModules
			with ImportRoutes
			with ImportCourses
			with ImportSitsStatuses
			with ImportModesOfAttendance
			with ImportAwards
			with ImportDisabilities
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringModuleImporterComponent
			with AutowiringCourseAndRouteServiceComponent
			with AutowiringCourseImporterComponent
			with AutowiringSitsStatusImporterComponent
			with AutowiringModeOfAttendanceImporterComponent
			with AutowiringAwardImporterComponent
			with AutowiringDisabilityImporterComponent
			with ImportSystemDataPermissions
			with ImportAcademicInformationDescription
			with Logging

	case class ImportResult(added: Int = 0, deleted: Int = 0, changed: Int = 0)

	case class ImportAcademicInformationResults(
		departments: ImportResult,
		modules: ImportResult,
		routes: ImportResult,
		courses: ImportResult,
		sitsStatuses: ImportResult,
		modesOfAttendance: ImportResult,
		awards: ImportResult,
		disabilities: ImportResult
	)

	def combineResults(results: Seq[ImportResult]): ImportResult =
		results.foldLeft(ImportResult())((acc, result) => ImportResult(
			added = acc.added + result.added,
			deleted = acc.deleted + result.deleted,
			changed = acc.changed + result.changed
		))

	def newModuleFrom(m: ModuleInfo, dept: Department): Module = {
		val module = new Module
		module.code = m.code
		module.name = m.name
		// TODO TAB-87 check child department rules and maybe sort it into a child department instead
		module.department = dept
		module
	}

	def newDepartmentFrom(d: DepartmentInfo, moduleAndDepartmentService: ModuleAndDepartmentService): Department = {
		val department = new Department
		department.code = d.code
		department.name = d.name
		d.parentCode.foreach { code =>
			// Don't try and handle a badly-specified code - just let the .get fail
			department.parent = moduleAndDepartmentService.getDepartmentByCode(code).get
		}
		d.filterName.foreach { name =>
			department.filterRule = new DepartmentFilterRuleUserType().convertToObject(name)
		}
		department
	}

	def newRouteFrom(r: RouteInfo, dept: Department): Route = {
		val route = new Route
		route.code = r.code
		route.name = r.name
		route.degreeType = r.degreeType
		route.department = dept
		route
	}
}

class ImportAcademicInformationCommandInternal extends CommandInternal[ImportAcademicInformationResults] with TaskBenchmarking {
	self: ImportDepartments with
		  ImportModules with
		  ImportRoutes with
		  ImportCourses with
		  ImportSitsStatuses with
		  ImportModesOfAttendance with
		  ImportAwards with
			ImportDisabilities =>

	def applyInternal() = transactional() {
		ImportAcademicInformationResults(
			departments = benchmarkTask("Import departments") { importDepartments() },
			modules = benchmarkTask("Import modules") { importModules() },
			routes = benchmarkTask("Import routes") { importRoutes() },
			courses = benchmarkTask("Import courses") { importCourses() },
			sitsStatuses = benchmarkTask("Import SITS status codes") { importSitsStatuses() },
			modesOfAttendance = benchmarkTask("Import modes of attendance") { importModesOfAttendance() },
			awards = benchmarkTask("Import awards") { importAwards() },
			disabilities = benchmarkTask("Import disabilities") { importDisabilities() }
		)
	}
}

trait ImportDepartments {
	self: ModuleAndDepartmentServiceComponent with ModuleImporterComponent with Logging =>

	def importDepartments(): ImportResult = {
		logger.info("Importing departments")
		val results = for (dept <- moduleImporter.getDepartments()) yield {
			moduleAndDepartmentService.getDepartmentByCode(dept.code) match {
				case None => {
					moduleAndDepartmentService.save(newDepartmentFrom(dept, moduleAndDepartmentService))

					ImportResult(added = 1)
				}
				case Some(dept) => {
					debug("Skipping %s as it is already in the database", dept.code)

					ImportResult()
				}
			}
		}

		combineResults(results)
	}
}

trait ImportModules {
	self: ModuleAndDepartmentServiceComponent with ModuleImporterComponent with Logging =>

	def importModules(): ImportResult = {
		logger.info("Importing modules")
		val results = for (dept <- moduleAndDepartmentService.allDepartments) yield {
			importModules(moduleImporter.getModules(dept.code), dept)
		}

		combineResults(results)
	}

	def importModules(modules: Seq[ModuleInfo], dept: Department): ImportResult = {
		val results = for (mod <- modules) yield {
			moduleAndDepartmentService.getModuleByCode(mod.code) match {
				case None => {
					debug("Mod code %s not found in database, so inserting", mod.code)
					moduleAndDepartmentService.saveOrUpdate(newModuleFrom(mod, dept))

					ImportResult(added = 1)
				}
				case Some(module) => {
					// HFC-354 Update module name if it changes.
					if (mod.name != module.name) {
						logger.info("Updating name of %s to %s".format(mod.code, mod.name))
						module.name = mod.name
						module.missingFromImportSince = null
						moduleAndDepartmentService.saveOrUpdate(module)

						ImportResult(changed = 1)
					} else {
						module.missingFromImportSince = null
						moduleAndDepartmentService.saveOrUpdate(module)

						ImportResult()
					}
				}
			}
		}

		val seenCodes = modules.map { _.code }
		moduleAndDepartmentService.stampMissingModules(dept, seenCodes)

		combineResults(results)
	}
}

trait ImportRoutes {
	self: ModuleAndDepartmentServiceComponent with CourseAndRouteServiceComponent with ModuleImporterComponent with Logging =>

	def importRoutes(): ImportResult = {
		logger.info("Importing routes")
		val results = for (dept <- moduleAndDepartmentService.allDepartments) yield {
			importRoutes(moduleImporter.getRoutes(dept.code), dept)
		}

		combineResults(results)
	}

	def importRoutes(routes: Seq[RouteInfo], dept: Department): ImportResult = {
		val results = for (rot <- routes) yield {
			courseAndRouteService.getRouteByCode(rot.code) match {
				case None => {
					debug("Route code %s not found in database, so inserting", rot.code)
					courseAndRouteService.save(newRouteFrom(rot, dept))

					ImportResult(added = 1)
				}
				case Some(route) => {
					// HFC-354 Update route name if it changes.
					if (rot.name != route.name) {
						logger.info("Updating name of %s to %s".format(rot.code, rot.name))
						route.name = rot.name
						courseAndRouteService.save(route)

						ImportResult(changed = 1)
					} else {
						ImportResult()
					}
				}
			}
		}

		combineResults(results)
	}
}

trait ImportCourses {
	self: CourseImporterComponent with Logging =>

	def importCourses(): ImportResult = {
		logger.info("Importing courses")
		courseImporter.importCourses()
	}
}

trait ImportSitsStatuses {
	self: SitsStatusImporterComponent with Logging =>

	def importSitsStatuses(): ImportResult = {
		logger.info("Importing SITS statuses")

		transactional() {
			val results = sitsStatusImporter.getSitsStatuses().map { _.apply()._2 }

			combineResults(results)
		}
	}
}

trait ImportModesOfAttendance {
	self: ModeOfAttendanceImporterComponent with Logging =>

	def importModesOfAttendance(): ImportResult = {
		logger.info("Importing modes of attendance")

		transactional() {
			val results = modeOfAttendanceImporter.getImportCommands().map { _.apply()._2 }

			combineResults(results)
		}
	}

}

trait ImportAwards {
	self: AwardImporterComponent with Logging =>

	def importAwards(): ImportResult = {
		logger.info("Importing awards")
		awardImporter.importAwards()
	}
}

trait ImportDisabilities {
	self: DisabilityImporterComponent with Logging =>

	def importDisabilities(): ImportResult = {
		logger.info("Importing disabilities")
		disabilityImporter.importDisabilities()
	}
}

trait ImportSystemDataPermissions extends RequiresPermissionsChecking {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}

trait ImportAcademicInformationDescription extends Describable[ImportAcademicInformationResults] {
	override lazy val eventName = "ImportAcademicInformation"

	def describe(d: Description) {}
	override def describeResult(d: Description, result: ImportAcademicInformationResults) {
		def importProperties(d: Description, name: String, result: ImportResult): Description = {
			d.properties(
				s"${name}Added" -> result.added,
				s"${name}Deleted" -> result.deleted,
				s"${name}Changed" -> result.changed
			)
		}

		importProperties(d, "departments", result.departments)
		importProperties(d, "modules", result.modules)
		importProperties(d, "routes", result.routes)
		importProperties(d, "courses", result.courses)
		importProperties(d, "sitsStatuses", result.sitsStatuses)
		importProperties(d, "modesOfAttendance", result.modesOfAttendance)
		importProperties(d, "awards", result.awards)
		importProperties(d, "disabilities", result.disabilities)
	}
}