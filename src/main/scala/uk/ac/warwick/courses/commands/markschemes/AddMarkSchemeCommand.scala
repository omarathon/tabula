package uk.ac.warwick.courses.commands.markschemes

import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.Transactions._
import org.springframework.validation.Errors
import org.springframework.beans.factory.annotation.Configurable

class AddMarkSchemeCommand(department: Department) extends ModifyMarkSchemeCommand(department) {
	
	// Copy properties to a new markscheme, save it transactionally, return it.
	def work() = {
		transactional() { 
			val markScheme = new MarkScheme(department)
			this.copyTo(markScheme)
			session.save(markScheme)
			markScheme
		}
	}
	
	// For validation. Not editing an existing markscheme so return None
	def currentMarkScheme = None

	override def validate(implicit errors: Errors) {
		super.validate(errors)
	}
	

	def describe(d: Description) = d.department(department)
}