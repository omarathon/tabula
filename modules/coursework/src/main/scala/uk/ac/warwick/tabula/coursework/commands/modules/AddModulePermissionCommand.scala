package uk.ac.warwick.tabula.coursework.commands.modules

import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Module
import scala.reflect.BeanProperty
import collection.JavaConversions._
import org.springframework.validation.Errors
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.tabula.permissions._

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
class AddModulePermissionCommand(val module: Module) extends Command[Unit] {
	
	PermissionCheck(Permissions.Module.ManagePermissions, module)
	module.ensureParticipantsGroup

	@BeanProperty var usercodes: JList[String] = ArrayList()
	@BeanProperty val permissionType: String = "Participate"

	var userLookup = Wire.auto[UserLookupService]

	def applyInternal() {
		transactional() {
			for (user <- usercodes) module.participants.addUser(user)
		}
	}

	def validate(errors: Errors) {
		val usercodeValidator = new UsercodeListValidator(usercodes, "usercodes") {
			override def alreadyHasCode = usercodes.find { module.participants.includes(_) }.isDefined
		}
		
		usercodeValidator.validate(errors)
	}

	private def alreadyHasCode = usercodes.find { module.participants.includes(_) }.isDefined

	private def usercodesEmpty = usercodes.find { _.hasText }.isEmpty

	def describe(d: Description) = d.module(module).properties(
		"usercodes" -> usercodes.mkString(","),
		"type" -> permissionType)

}

