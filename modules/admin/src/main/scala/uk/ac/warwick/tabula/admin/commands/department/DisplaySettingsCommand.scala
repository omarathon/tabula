package uk.ac.warwick.tabula.admin.commands.department

import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Department.Settings
import uk.ac.warwick.tabula.services.{ AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent, ModuleAndDepartmentService }
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod
import uk.ac.warwick.tabula.system.permissions.{ PermissionsChecking, RequiresPermissionsChecking }
import org.springframework.validation.{ BindingResult, Errors }
import uk.ac.warwick.tabula.system.BindListener
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.services.RelationshipServiceComponent
import uk.ac.warwick.tabula.services.AutowiringRelationshipServiceComponent

trait DisplaySettingsCommand extends CommandInternal[Department] with DisplaySettingsCommandState {
	def init(): Unit
}

object DisplaySettingsCommand {
	def apply(department: Department) =
		new DisplaySettingsCommandInternal(department)
			with ComposableCommand[Department]
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringRelationshipServiceComponent
			with DisplaySettingsCommandDescription
			with DisplaySettingsCommandPermissions
}

trait DisplaySettingsCommandState {
	val department: Department
}

class DisplaySettingsCommandInternal(val department: Department) extends DisplaySettingsCommand with CommandInternal[Department]
	with SelfValidating with BindListener with DisplaySettingsCommandState {

	this: ModuleAndDepartmentServiceComponent with RelationshipServiceComponent =>

	var showStudentName = department.showStudentName
	var plagiarismDetection = department.plagiarismDetectionEnabled
	var turnitinExcludeBibliography = department.turnitinExcludeBibliography
	var turnitinExcludeQuotations = department.turnitinExcludeQuotations
	var turnitinExcludeSmallMatches: Boolean = _ // not saved as part of the settings - just used in the UI
	var turnitinSmallMatchWordLimit = department.turnitinSmallMatchWordLimit
	var turnitinSmallMatchPercentageLimit = department.turnitinSmallMatchPercentageLimit
	var assignmentInfoView = department.assignmentInfoView
	var weekNumberingSystem = department.weekNumberingSystem
	var autoGroupDeregistration = department.autoGroupDeregistration
	var defaultGroupAllocationMethod = department.defaultGroupAllocationMethod.dbValue
	var studentRelationshipDisplayed: JMap[String, JBoolean] =
		JHashMap(department.studentRelationshipDisplayed.map {
			case (id, bString) => (id -> java.lang.Boolean.valueOf(bString))
		})

	def init() {
		relationshipService.allStudentRelationshipTypes.foreach { relationshipType =>
			if (!studentRelationshipDisplayed.containsKey(relationshipType.id))
				studentRelationshipDisplayed.put(relationshipType.id, relationshipType.defaultDisplay)
		}
	}

	override def applyInternal() = transactional() {
		department.showStudentName = showStudentName
		department.plagiarismDetectionEnabled = plagiarismDetection
		department.turnitinExcludeBibliography = turnitinExcludeBibliography
		department.turnitinExcludeQuotations = turnitinExcludeQuotations
		department.turnitinSmallMatchWordLimit = turnitinSmallMatchWordLimit
		department.turnitinSmallMatchPercentageLimit = turnitinSmallMatchPercentageLimit
		department.assignmentInfoView = assignmentInfoView
		department.autoGroupDeregistration = autoGroupDeregistration
		department.defaultGroupAllocationMethod = SmallGroupAllocationMethod(defaultGroupAllocationMethod)
		department.weekNumberingSystem = weekNumberingSystem
		department.studentRelationshipDisplayed = studentRelationshipDisplayed.asScala.map { case (id, bool) => (id -> Option(bool).getOrElse(false).toString) }.toMap

		moduleAndDepartmentService.save(department)
		department
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

trait DisplaySettingsCommandPermissions extends RequiresPermissionsChecking {
	this: DisplaySettingsCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ManageDisplaySettings, department)
	}
}

trait DisplaySettingsCommandDescription extends Describable[Department] {
	this: DisplaySettingsCommandState =>
	// describe the thing that's happening.
	override def describe(d: Description) =
		d.department(department)
}

