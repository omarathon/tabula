package uk.ac.warwick.tabula.commands.cm2.assignments.extensions

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module}
import uk.ac.warwick.tabula.data.model.forms.ExtensionState
import uk.ac.warwick.tabula.helpers.coursework.ExtensionGraph
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

case class FilterExtensionResults(
	extensions: Seq[ExtensionGraph],
	total: Int
)

object FilterExtensionsCommand {
	def apply(user: CurrentUser) =
		new FilterExtensionsCommandInternal(user)
			with ComposableCommand[FilterExtensionResults]
			with FilterExtensionsPermissions
			with AutowiringUserLookupComponent
			with AutowiringExtensionServiceComponent
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringTermServiceComponent
			with ReadOnly with Unaudited
}

class FilterExtensionsCommandInternal(val user: CurrentUser) extends CommandInternal[FilterExtensionResults]
	with FilterExtensionsState with BindListener with TaskBenchmarking {

	this: UserLookupComponent with ExtensionServiceComponent with ModuleAndDepartmentServiceComponent
		with TermServiceComponent =>

	private def departmentsWithPermssion =
		moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Department.ManageExtensionSettings)
	private def modulesWithPermssion =
		moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.Administer)
	private def modulesInDepartmentsWithPermission =
		moduleAndDepartmentService.modulesInDepartmentsWithPermission(user, Permissions.Department.ManageExtensionSettings)

	lazy val allModules = (modulesWithPermssion ++ modulesInDepartmentsWithPermission).toSeq
	lazy val allDepartments = (departmentsWithPermssion ++ allModules.map(_.adminDepartment)).toSeq

	def applyInternal() = {

		val totalResults = benchmarkTask("countStudentsByRestrictions") { extensionService.countFilteredExtensions(
			restrictions = restrictions
		)}

		val orders = if (sortOrder.isEmpty) defaultOrder else sortOrder

		val extensions = benchmarkTask("findStudentsByRestrictions") { extensionService.filterExtensions(
			restrictions,
			buildOrders(orders.asScala),
			extensionsPerPage,
			extensionsPerPage * (page-1)
		)}

		val graphs = extensions.map(e => ExtensionGraph(e, userLookup.getUserByUserId(e.userId)))
		FilterExtensionResults(graphs, totalResults)
	}

	def onBind(result: BindingResult) {
		// TODO - Default filter here ?
	}
}


trait FilterExtensionsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FilterExtensionsState =>

	def permissionsCheck(p: PermissionsChecking) {
		// TODO - perm check on all depts / modules and assignments filtered ?
		p.PermissionCheck(Permissions.Extension.Search)
	}
}

trait FilterExtensionsState extends FiltersExtensions {

	self: TermServiceComponent =>

	var page = 1
	var extensionsPerPage = 50
	var defaultOrder: JList[Order] = Seq(desc("requestedOn")).asJava
	var sortOrder: JList[Order] = JArrayList()

	var times: JList[TimeFilter] = JArrayList()
	var states: JList[ExtensionState] = JArrayList()
	var assignments: JList[Assignment] = JArrayList()
	var modules: JList[Module] = JArrayList()
	var departments: JList[Department] = JArrayList()

	val user: CurrentUser
	val allModules: Seq[Module]
	val allDepartments: Seq[Department]
	lazy val allStates = ExtensionState.all
	lazy val allTimes = TimeFilter.all
}