package uk.ac.warwick.tabula.admin.commands.department
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Department.Settings
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.spring.Wire


class DisplaySettingsCommand (val department:Department) extends Command[Unit] with SelfValidating {
	
	PermissionCheck(Permissions.Department.ManageDisplaySettings, department)
	
	var departmentService = Wire[ModuleAndDepartmentService]

	var showStudentName = department.showStudentName
	var plagiarismDetection = department.plagiarismDetectionEnabled
	var assignmentInfoView = department.assignmentInfoView
	var weekNumberingSystem = department.weekNumberingSystem

	override def applyInternal() = transactional() {
		department ++= (
			Settings.ShowStudentName -> showStudentName,
			Settings.PlagiarismDetection -> plagiarismDetection,
			Settings.AssignmentInfoView -> assignmentInfoView,
			Settings.WeekNumberingSystem -> weekNumberingSystem
		)
		
		departmentService.save(department)
	}

	// describe the thing that's happening.
	override def describe(d:Description) =
		d.department(department)

	override def validate(errors:Errors) {}
}