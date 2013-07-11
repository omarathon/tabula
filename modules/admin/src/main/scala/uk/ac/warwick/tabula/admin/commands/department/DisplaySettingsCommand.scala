package uk.ac.warwick.tabula.admin.commands.department
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Department.Settings
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent, ModuleAndDepartmentService}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

object DisplaySettingsCommand{
	def apply(department:Department) =
		new DisplaySettingsCommandInternal(department)
			with ComposableCommand[Unit]
			with AutowiringModuleAndDepartmentServiceComponent
	    with DisplaySettingsCommandDescription
	    with DisplaySettingsCommandPermissions
}

trait DisplaySettingsCommandState{
	val department:Department
}

class DisplaySettingsCommandInternal (val department:Department) extends CommandInternal[Unit]  with DisplaySettingsCommandState  {
	this:ModuleAndDepartmentServiceComponent =>
	
  var showStudentName = department.showStudentName
	var plagiarismDetection = department.plagiarismDetectionEnabled
	var assignmentInfoView = department.assignmentInfoView
	var weekNumberingSystem = department.weekNumberingSystem
	var defaultGroupAllocationMethod = department.defaultGroupAllocationMethod.dbValue

	override def applyInternal() = transactional() {
		department ++= (
			Settings.ShowStudentName -> showStudentName,
			Settings.PlagiarismDetection -> plagiarismDetection,
			Settings.AssignmentInfoView -> assignmentInfoView,
			Settings.DefaultGroupAllocationMethod -> SmallGroupAllocationMethod(defaultGroupAllocationMethod).dbValue,
			Settings.WeekNumberingSystem -> weekNumberingSystem
		)
		
		moduleAndDepartmentService.save(department)
	}
}

trait DisplaySettingsCommandPermissions extends RequiresPermissionsChecking{
	this:DisplaySettingsCommandState=>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ManageDisplaySettings, department)

	}
}
trait DisplaySettingsCommandDescription extends Describable[Unit]{
	this:DisplaySettingsCommandState=>
	// describe the thing that's happening.
	override def describe(d:Description) =
		d.department(department)
}

