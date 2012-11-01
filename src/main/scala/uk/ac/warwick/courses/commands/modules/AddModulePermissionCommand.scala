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
class AddModulePermissionCommand extends Command[Unit] {

	@BeanProperty var module: Module = _
	@BeanProperty var usercodes: JList[String] = _
	@BeanProperty val permissionType: String = "Participate"

	@Autowired var userLookup: UserLookupService = _

	@Transactional
	def work() {
		for (user <- usercodes)
			module.participants.addUser(user)
	}

	def validate(errors: Errors) {
		if (usercodesEmpty) {
			errors.rejectValue("usercodes", "NotEmpty")
		} else if (alreadyHasCode) {
			errors.rejectValue("usercodes", "userId.duplicate")
		} else {
			val anonUsers = userLookup.getUsersByUserIds(usercodes).values().find { !_.isFoundUser() }
			for (user <- anonUsers) {
				errors.rejectValue("usercodes", "userId.notfound.specified", Array(user.getUserId), "")
			}
		}
	}

	private def alreadyHasCode = usercodes.find { module.participants.includes(_) }.isDefined

	private def usercodesEmpty = usercodes.find { StringUtils.hasText(_) }.isEmpty

	def describe(d: Description) = d.module(module).properties(
		"usercodes" -> usercodes.mkString(","),
		"type" -> permissionType)

}