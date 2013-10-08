package uk.ac.warwick.tabula.admin.commands.routes
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.data.model.Route

/** Arranges routes between a department and its child departments. */
class SortRoutesCommand(val department: Department) extends Command[Unit] with SelfValidating {

	var mads = Wire[ModuleAndDepartmentService]

	PermissionCheck(Permissions.Department.ArrangeRoutes, department)

	/** Mapping from departments to an ArrayList containing route IDs. */
	var mapping = JMap[Department, JList[Route]]()
	for (dept <- (departments))
		mapping.put(dept, JArrayList())

	def departments = (department :: department.children.asScala.toList)

	// Only called on initial form view
	def populate() {
		for (dept <- (departments))
			mapping.put(dept, dept.routes)
	}

	// Purely for use by Freemarker as it can't access map values unless the key is a simple value.
	// Do not modify the returned value!
	def mappingByCode = mapping.asScala.map {
		case (dept, routes) => (dept.code, routes)
	}

	final def applyInternal() = transactional() {
		for ((dept, routes) <- mapping.asScala) {
			dept.routes.clear()
			dept.routes.addAll(routes)
			for (r <- routes) r.department = dept
			mads.save(dept)
		}
	}

	def validate(errors: Errors) {
		val mappingMap = mapping.asScala
		val allDepartments = mappingMap.keys
		val currentRoutes = allDepartments.map(_.routes.asScala).flatten.toList
		val newRoutes = mappingMap.values.map(_.asScala).flatten.toList.filter(validRoute)

		/* These next errors shouldn't really be possible from the UI unless there's a bug. */

		// Disallow submitting unrelated Departments
		if (!mappingMap.keys.forall( d => departments.contains(d) )) {
			errors.reject("sortRoutes.departments.invalid")
		}

		// Disallow referencing any Routes from other departments.
		// Also disallow removing routes from the ones we started with.
		if (newRoutes.sorted != currentRoutes.sorted) {
			errors.reject("sortRoutes.routes.invalid")
		}
	}

	// Sort all the lists of routes by code.
	def sort() {
		// Because sortBy is not an in-place sort, we have to replace the lists entirely.
		// Alternative is Collections.sort or math.Sorting but these would be more code.
		for ((dept, routes) <- mapping.asScala) {
			mapping.put(dept, routes.asScala.toList.filter(validRoute).sorted)
		}
	}

	private def validRoute(route: Route) = route.code != null

	def describe(d: Description) {
		d.department(department)
	}

}
