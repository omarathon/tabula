package uk.ac.warwick.tabula.coursework.commands.departments
import scala.reflect.BeanProperty

import org.springframework.validation.Errors

import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.StringUtils._


class DisplaySettingsCommand (val department:Department) extends Command[Unit] {
	
	PermissionCheck(Permissions.Department.ManageDisplaySettings(), department)

	@BeanProperty var showStudentName:JBoolean =_

	def validate(errors:Errors){

	}

	def copySettings() {
		showStudentName = department.showStudentName
	}

	override def applyInternal() {
		transactional() {
			department.showStudentName = showStudentName
		}
	}

	// describe the thing that's happening.
	override def describe(d:Description) {
		d.properties("department" -> department.code)
	}
}