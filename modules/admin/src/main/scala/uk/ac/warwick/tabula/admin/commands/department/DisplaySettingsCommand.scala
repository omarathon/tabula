package uk.ac.warwick.tabula.admin.commands.department
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Department.Settings
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent, ModuleAndDepartmentService}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.system.BindListener

object DisplaySettingsCommand{
	def apply(department:Department) =
		new DisplaySettingsCommandInternal(department)
			with ComposableCommand[Unit]
			with AutowiringModuleAndDepartmentServiceComponent
	    with DisplaySettingsCommandDescription
	    with DisplaySettingsCommandPermissions
}

trait DisplaySettingsCommandState {
	val department:Department
}

class DisplaySettingsCommandInternal (val department:Department) extends CommandInternal[Unit]
	with SelfValidating with BindListener with DisplaySettingsCommandState {

	this:ModuleAndDepartmentServiceComponent =>
	
  var showStudentName = department.showStudentName
	var plagiarismDetection = department.plagiarismDetectionEnabled
	var turnitinExcludeBibliography = department.turnitinExcludeBibliography
	var turnitinExcludeQuotations = department.turnitinExcludeQuotations
	var turnitinExcludeSmallMatches: Boolean = _ // not saved as part of the settings - just used in the UI
	var turnitinSmallMatchWordLimit = department.turnitinSmallMatchWordLimit
	var turnitinSmallMatchPercentageLimit = department.turnitinSmallMatchPercentageLimit
	var assignmentInfoView = department.assignmentInfoView
	var weekNumberingSystem = department.weekNumberingSystem
	var defaultGroupAllocationMethod = department.defaultGroupAllocationMethod.dbValue

	override def applyInternal() = transactional() {
		department.showStudentName = showStudentName
		department.plagiarismDetectionEnabled =  plagiarismDetection
		department.turnitinExcludeBibliography = turnitinExcludeBibliography
		department.turnitinExcludeQuotations = turnitinExcludeQuotations
		department.turnitinSmallMatchWordLimit = turnitinSmallMatchWordLimit
		department.turnitinSmallMatchPercentageLimit = turnitinSmallMatchPercentageLimit
		department.assignmentInfoView = assignmentInfoView
		department.defaultGroupAllocationMethod = SmallGroupAllocationMethod(defaultGroupAllocationMethod)
		department.weekNumberingSystem = weekNumberingSystem

		moduleAndDepartmentService.save(department)
	}

	override def onBind(result: BindingResult) {
		turnitinExcludeSmallMatches = (turnitinSmallMatchWordLimit != 0 || turnitinSmallMatchPercentageLimit != 0)
	}

	override def validate(errors: Errors) {
		if (turnitinSmallMatchWordLimit < 0) {
			errors.rejectValue("turnitinSmallMatchWordLimit", "department.settings.turnitinSmallMatchWordLimit")
		}
		if (turnitinSmallMatchPercentageLimit < 0 || turnitinSmallMatchPercentageLimit > 100) {
			errors.rejectValue("turnitinSmallMatchPercentageLimit", "department.settings.turnitinSmallMatchPercentageLimit")
		}
		if (turnitinSmallMatchWordLimit != 0 && turnitinSmallMatchPercentageLimit != 0) {
			errors.rejectValue("turnitinExcludeSmallMatches", "department.settings.turnitinSmallMatchSingle")
		}
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

