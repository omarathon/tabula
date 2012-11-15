package uk.ac.warwick.courses.commands.markschemes

import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.Transactions._
import org.springframework.validation.Errors

/** Edit an existing markscheme. */
class EditMarkSchemeCommand(department: Department, val markScheme: MarkScheme) extends ModifyMarkSchemeCommand(department) {

	// fill in the properties on construction
	copyFrom(markScheme)

	def work() = {
		transactional() {
			this.copyTo(markScheme)
			session.update(markScheme)
			markScheme
		}
	}
	
	def currentMarkScheme = Some(markScheme)

	override def validate(implicit errors: Errors) {
		super.validate(errors)
	}

	def describe(d: Description) = d.department(department).markScheme(markScheme)
}