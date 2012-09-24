package uk.ac.warwick.courses.commands.modules

import uk.ac.warwick.courses
import uk.ac.warwick.courses._

import commands.{ Description, SelfValidating }
import data.Daoisms
import data.model._

import collection.JavaConversions._
import reflect.BeanProperty
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.{ ValidationUtils, Errors }
import org.springframework.beans.factory.annotation.Configurable

@Configurable
class AddModuleCommand extends ModifyModuleCommand with Daoisms with SelfValidating {

	@BeanProperty var department: Department = _

	@BeanProperty var code: String = _
	@BeanProperty var name: String = _

	def sanitisedCode = Option(code).map(_.toLowerCase).orNull

	@Transactional
	def apply() = {
		val module = new Module()
		module.department = department
		module.name = name
		module.code = sanitisedCode
		session.save(module)
		module
	}

	def validate(implicit errors: Errors) {
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

	def describe(d: Description) {
		d.department(department)
		d.property("moduleCode", sanitisedCode)
		d.property("moduleName", name)
	}
}
