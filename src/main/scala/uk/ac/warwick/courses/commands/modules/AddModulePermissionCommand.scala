package uk.ac.warwick.courses.commands.modules

import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.data.model.Module
import scala.reflect.BeanProperty
import collection.JavaConversions._
import org.springframework.validation.Errors
import uk.ac.warwick.courses.services.UserLookupService
import uk.ac.warwick.courses.data.Transactions._
import uk.ac.warwick.util.core.StringUtils
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.courses.helpers.ArrayList

/**
 * Command for adding permissions to a module.
 * 
 * Adding users one-by-one like this is probably not how we'd do it now. We'd have
 * a client-side view to add users using the AJAX picker, and then a final submit to
 * save all the changes. Avoids the need for a 
 * 
 * You might still use this version for places where you need to deal with a lot of
 * users (hundreds or more) where it can get slow to submit hundreds of values at once.
 */
class AddModulePermissionCommand extends Command[Unit] {

	@BeanProperty var module: Module = _
	@BeanProperty var usercodes: JList[String] = ArrayList()
	@BeanProperty val permissionType: String = "Participate"

	var userLookup = Wire.auto[UserLookupService]

	def work() {
		transactional() {
			for (user <- usercodes) module.participants.addUser(user)
		}
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