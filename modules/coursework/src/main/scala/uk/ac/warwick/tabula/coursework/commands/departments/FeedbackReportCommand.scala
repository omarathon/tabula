package uk.ac.warwick.tabula.coursework.commands.departments
import org.springframework.validation.Errors

import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.actions.Manage
import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.StringUtils._


class FeedbackReportCommand (val department:Department, val features:Features) extends Command[Unit] {
	
	PermissionsCheck(Manage(department))

	def validate(errors:Errors){

	}
	
	override def applyInternal() {

	}

	// describe the thing that's happening.
	override def describe(d:Description) {
		d.properties("department" -> department.code)
	}
}