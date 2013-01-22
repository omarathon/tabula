package uk.ac.warwick.tabula.coursework.commands.modules

import uk.ac.warwick.tabula
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.Transactions._
import commands.{ Description, SelfValidating }
import data.Daoisms
import data.model._
import collection.JavaConversions._
import reflect.BeanProperty
import org.springframework.validation.{ ValidationUtils, Errors }
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.actions.Sysadmin


class AddModuleCommand extends ModifyModuleCommand with Daoisms with SelfValidating {
	
	PermissionsCheck(Sysadmin())

	@BeanProperty var department: Department = _

	@BeanProperty var code: String = _
	@BeanProperty var name: String = _

	def sanitisedCode = Option(code).map(_.toLowerCase).orNull

	def applyInternal() = transactional() {
		val module = new Module()
		module.department = department
		module.name = name
		module.code = sanitisedCode
		session.save(module)
		module
	}

	def validate(errors: Errors) {
		//TODO proper department checks before we open this up to non-sysadmins

		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "code", "NotEmpty")
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "NotEmpty")

		ValidationUtils.rejectIfEmpty(errors, "department", "NotEmpty")

		// check for duplicate name or code
		if (department != null) {
			if (code != null && department.modules.exists { _.code == code.toLowerCase }) {
				errors.rejectValue("code", "code.duplicate.module")
			}
			if (name != null && department.modules.exists { _.name equalsIgnoreCase name }) {
				errors.rejectValue("name", "name.duplicate.module")
			}
		}

	}

	def describe(d: Description) = d
		.department(department)
		.property("moduleCode", sanitisedCode)
		.property("moduleName", name)

}
