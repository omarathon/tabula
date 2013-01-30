package uk.ac.warwick.tabula.scheduling.commands.imports

import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.Daoisms
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.scheduling.services.DepartmentInfo
import uk.ac.warwick.tabula.scheduling.services.ModuleInfo
import uk.ac.warwick.tabula.scheduling.services.ModuleImporter
import uk.ac.warwick.tabula.scheduling.services.RouteInfo
import uk.ac.warwick.tabula.permissions._

class ImportModulesCommand extends Command[Unit] with Logging with Daoisms {
	import ImportModulesCommand._
	
	PermissionCheck(Permissions.ImportSystemData)

	var moduleImporter = Wire.auto[ModuleImporter]
	var moduleService = Wire.auto[ModuleAndDepartmentService]

	def applyInternal() {
		transactional() {
			importDepartments
			importModules
			importRoutes
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
			moduleService.getRouteByCode(rot.code) match {
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
				case None => session.save(newDepartmentFrom(dept))
				case Some(dept) => {debug("Skipping %s as it is already in the database", dept.code) }
			}
		}
	}

}

object ImportModulesCommand {

	def newModuleFrom(m: ModuleInfo, dept: Department): Module = {
		val module = new Module
		module.code = m.code
		module.name = m.name
		//      module.members.baseWebgroup = m.group
		module.department = dept
		module
	}

	def newDepartmentFrom(d: DepartmentInfo): Department = {
		val department = new Department
		department.code = d.code
		department.name = d.name
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