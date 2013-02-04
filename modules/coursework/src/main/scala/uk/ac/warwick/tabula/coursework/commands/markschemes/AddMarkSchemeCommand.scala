package uk.ac.warwick.tabula.coursework.commands.markschemes

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.permissions._

class AddMarkSchemeCommand(department: Department) extends ModifyMarkSchemeCommand(department) {
	
	PermissionCheck(Permissions.MarkScheme.Create, department)

	// Copy properties to a new markscheme, save it transactionally, return it.
	def applyInternal() = {
		transactional() { 
			val markScheme = new MarkScheme(department)
			this.copyTo(markScheme)
			session.save(markScheme)
			markScheme
		}
	}
	
	// For validation. Not editing an existing markscheme so return None
	def currentMarkScheme = None

	def contextSpecificValidation(errors:Errors){}

	override def validate(errors: Errors) {
		super.validate(errors)
	}
	

	def describe(d: Description) = d.department(department)
}