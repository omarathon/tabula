package uk.ac.warwick.tabula.admin.commands.modules
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService

/** Arranges modules between a department and its child departments. */
class SortModulesCommand(val department: Department) extends Command[Unit] with SelfValidating {

	var mads = Wire[ModuleAndDepartmentService]

	PermissionCheck(Permissions.Department.ArrangeModules, department)

	/** Mapping from departments to an ArrayList containing module IDs. */
	var mapping = JMap[Department, JList[Module]]()
	for (dept <- (departments))
		mapping.put(dept, JArrayList())

	def departments = (department :: department.children.asScala.toList)

	// Only called on initial form view
	def populate() {
		for (dept <- (departments))
			mapping.put(dept, dept.modules)
	}

	// Purely for use by Freemarker as it can't access map values unless the key is a simple value.
	// Do not modify the returned value!
	def mappingByCode = mapping.asScala.map {
		case (dept, modules) => (dept.code, modules)
	}

	final def applyInternal() = transactional() {
		for ((dept, modules) <- mapping.asScala) {
			dept.modules.clear()
			dept.modules.addAll(modules)
			for (m <- modules) m.department = dept
			mads.save(dept)
		}
	}

	def validate(errors: Errors) {
		val mappingMap = mapping.asScala
		val allDepartments = mappingMap.keys
		val currentModules = allDepartments.map(_.modules.asScala).flatten.toList
		val newModules = mappingMap.values.map(_.asScala).flatten.toList.filter(validModule)

		/* These next errors shouldn't really be possible from the UI unless there's a bug. */

		// Disallow submitting unrelated Departments
		if (!mappingMap.keys.forall( d => departments.contains(d) )) {
			errors.reject("sortModules.departments.invalid")
		}

		// Disallow referencing any Modules from other departments.
		// Also disallow removing modules from the ones we started with.
		if (newModules.sorted != currentModules.sorted) {
			errors.reject("sortModules.modules.invalid")
		}
	}

	// Sort all the lists of modules by code.
	def sort() {
		// Because sortBy is not an in-place sort, we have to replace the lists entirely.
		// Alternative is Collections.sort or math.Sorting but these would be more code.
		for ((dept, modules) <- mapping.asScala) {
			mapping.put(dept, modules.asScala.toList.filter(validModule).sorted)
		}
	}

	private def validModule(module: Module) = module.code != null

	def describe(d: Description) {
		d.department(department)
	}

}
