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


class FeedbackReportCommand (val department:Department, val features:Features) extends Command[Unit] {

	def validate(errors:Errors){

	}
	
	override def applyInternal() {

	}

	// describe the thing that's happening.
	override def describe(d:Description) {
		d.properties("department" -> department.code)
	}
}