package uk.ac.warwick.tabula.coursework.commands.departments

import uk.ac.warwick.tabula.data.model.{UserGroup, Department}
import uk.ac.warwick.tabula.commands.{Description, Command}
import reflect.BeanProperty
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.validators.UsercodeListValidator


class DisplaySettingsCommand (val department:Department, val features:Features) extends Command[Unit] {

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