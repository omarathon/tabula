package uk.ac.warwick.tabula.coursework.commands.departments
import scala.beans.BeanProperty
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Department.Settings
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.commands.SelfValidating


class DisplaySettingsCommand (val department:Department) extends Command[Unit] with SelfValidating {
	
	PermissionCheck(Permissions.Department.ManageDisplaySettings, department)

	@BeanProperty var showStudentName:Boolean = department.isShowStudentName
	@BeanProperty var plagiarismDetection:Boolean = department.isPlagiarismDetectionEnabled

	override def applyInternal() {
		transactional() {
			department ++= (
				Settings.ShowStudentName -> showStudentName,
				Settings.PlagiarismDetection -> plagiarismDetection
			)
		}
	}

	// describe the thing that's happening.
	override def describe(d:Description) {
		d.properties("department" -> department.code)
	}

	override def validate(errors:Errors){

	}
}