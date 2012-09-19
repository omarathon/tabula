package uk.ac.warwick.courses.commands.modules

import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.data.model.Module
import scala.reflect.BeanProperty
import collection.JavaConversions._
import org.springframework.validation.Errors
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.UserLookupService
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.util.core.StringUtils

@Configurable
class RemoveModulePermissionCommand extends Command[Unit] {

	@BeanProperty var module: Module = _
	@BeanProperty var usercodes: JList[String] = _
	@BeanProperty val permissionType: String = "Participate"

	@Autowired var userLookup: UserLookupService = _

	@Transactional
	def apply {
		for (user <- usercodes)
			module.participants.removeUser(user)
	}

	def validate(errors: Errors) {
		if (usercodesEmpty) {
			errors.rejectValue("usercodes", "NotEmpty")
		} else {
			for (code <- usercodes) {
				if (!module.participants.includes(code)) {
					errors.rejectValue("usercodes", "userId.notingroup", Array(code), "")
				}
			}
		}
	}

	private def usercodesEmpty = usercodes.find { StringUtils.hasText(_) }.isEmpty

	def describe(d: Description) = d.module(module).properties(
		"usercodes" -> usercodes.mkString(","),
		"type" -> permissionType)

}