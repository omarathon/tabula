package uk.ac.warwick.tabula.scheduling.commands.imports

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.{ DepartmentDao, Daoisms}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.scheduling.services.DepartmentInfo
import uk.ac.warwick.tabula.scheduling.services.ModuleInfo
import uk.ac.warwick.tabula.scheduling.services.ModuleImporter
import uk.ac.warwick.tabula.scheduling.services.RouteInfo
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.CourseAndRouteService
import uk.ac.warwick.tabula.data.model.Department.FilterRule
import uk.ac.warwick.tabula.scheduling.services.AwardImporter
import uk.ac.warwick.tabula.scheduling.services.CourseImporter
import uk.ac.warwick.tabula.scheduling.services.ModeOfAttendanceImporter
import uk.ac.warwick.tabula.scheduling.services.SitsStatusesImporter

class ImportModulesCommand extends Command[Unit] with Logging with Daoisms {
	import ImportModulesCommand._

	PermissionCheck(Permissions.ImportSystemData)

	var moduleImporter = Wire[ModuleImporter]
	var moduleService = Wire[ModuleAndDepartmentService]
	var courseAndRouteService = Wire[CourseAndRouteService]
	var departmentDao = Wire[DepartmentDao]
	var sitsStatusesImporter = Wire[SitsStatusesImporter]
	var modeOfAttendanceImporter = Wire[ModeOfAttendanceImporter]
	var courseImporter = Wire[CourseImporter]
	var awardImporter = Wire[AwardImporter]

	def applyInternal() {
		transactional() {
			importDepartments
			importModules
			importRoutes
			importSitsStatuses
			importModeOfAttendances
			importCourses
			importAwards
		}
	}

	def describe(d: Description) {

	}

	def importModules {
		logger.info("Importing modules")
		for (dept <- moduleService.allDepartments) {
			importModules(moduleImporter.getModules(dept.code), dept)
		}
	}

	def importModules(modules: Seq[ModuleInfo], dept: Department) {
		for (mod <- modules) {
			moduleService.getModuleByCode(mod.code) match {
				case None => {
					debug("Mod code %s not found in database, so inserting", mod.code)
					session.saveOrUpdate(newModuleFrom(mod, dept))
				}
				case Some(module) => {
					// HFC-354 Update module name if it changes.
					if (mod.name != module.name) {
						logger.info("Updating name of %s to %s".format(mod.code, mod.name))
						module.name = mod.name
						session.saveOrUpdate(module)
					}
				}
			}
		}
	}

	def importRoutes {
		logger.info("Importing routes")
		for (dept <- moduleService.allDepartments) {
			importRoutes(moduleImporter.getRoutes(dept.code), dept)
		}
	}

	def importRoutes(routes: Seq[RouteInfo], dept: Department) {
		for (rot <- routes) {
			courseAndRouteService.getRouteByCode(rot.code) match {
				case None => {
					debug("Route code %s not found in database, so inserting", rot.code)
					session.saveOrUpdate(newRouteFrom(rot, dept))
				}
				case Some(route) => {
					// HFC-354 Update route name if it changes.
					if (rot.name != route.name) {
						logger.info("Updating name of %s to %s".format(rot.code, rot.name))
						route.name = rot.name
						session.saveOrUpdate(route)
					}
				}
			}
		}
	}

	def importDepartments {
		logger.info("Importing departments")
		for (dept <- moduleImporter.getDepartments) {
			moduleService.getDepartmentByCode(dept.code) match {
				case None => session.save(newDepartmentFrom(dept,departmentDao))
				case Some(dept) => {debug("Skipping %s as it is already in the database", dept.code) }
			}
		}
	}
	
	def importSitsStatuses {
		logger.info("Importing SITS statuses")

		transactional() {
			sitsStatusesImporter.getSitsStatuses map { _.apply }

			session.flush
			session.clear
		}
	}

	def importModeOfAttendances {
		logger.info("Importing modes of attendance")

		transactional() {
			modeOfAttendanceImporter.getImportCommands foreach { _.apply() }

			session.flush
			session.clear
		}
	}
	
	def importCourses {
		courseImporter.importCourses
	}
	
	def importAwards {
		awardImporter.importAwards
	}

}

object ImportModulesCommand {

	def newModuleFrom(m: ModuleInfo, dept: Department): Module = {
		val module = new Module
		module.code = m.code
		module.name = m.name
		// TODO TAB-87 check child department rules and maybe sort it into a child department instead
		module.department = dept
		module
	}

	def newDepartmentFrom(d: DepartmentInfo, dao:DepartmentDao): Department = {
		val department = new Department
		department.code = d.code
		department.name = d.name
		d.parentCode foreach { code =>
			// Don't try and handle a badly-specified code - just let the .get fail
			department.parent =  dao.getByCode(code).get
		}
		d.filterName foreach  {name =>
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
