package uk.ac.warwick.tabula.admin.commands.modules
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.commands.{Description, SelfValidating}
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model._
import collection.JavaConversions._
import reflect.BeanProperty
import org.springframework.validation.{ ValidationUtils, Errors }
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.Command


class AddModuleCommand(@BeanProperty val department: Department) extends Command[Module] with Daoisms with SelfValidating {
	
	PermissionCheck(Permissions.Module.Create, department)

	var code: String = _
	var name: String = _

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
				errors.rejectValue("code", "code.duplicate.module", Array(code.toUpperCase()), "")
			}
			if (name != null && department.modules.exists { _.name equalsIgnoreCase name }) {
				errors.rejectValue("name", "name.duplicate.module", Array(name), "")
			}
		}

	}

	def describe(d: Description) = d
		.department(department)
		.property("moduleCode", sanitisedCode)
		.property("moduleName", name)

}
