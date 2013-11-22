package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent, AssignmentService}

class CreateExtensionFixtureCommand extends CommandInternal[Extension] {
	this: TransactionalComponent with UserLookupComponent =>

	var assignmentService: AssignmentService = Wire[AssignmentService]

	var userId: String = _
	var assignmentId: String = _
	var approved: Boolean = _

	protected def applyInternal() = {
		transactional() {
			val now = new DateTime()
			val user = userLookup.getUserByUserId(userId)
			val e = new Extension()
			e.universityId = user.getWarwickId
			e.userId = user.getUserId
			e.requestedOn = now
			e.requestedExpiryDate = now.plusMonths(3)
			e.reason = "For use in fixture."
			e.assignment = assignmentService.getAssignmentById(assignmentId).get

			if (approved) {
				e.approved = true
				e.approvedOn = now.plusMillis(5) // superquick
			}

			// make sure to manually create the inverse relationship
			e.assignment.extensions.add(e)

			e
		}
	}
}

object CreateExtensionFixtureCommand {
	def apply() = {
		new CreateExtensionFixtureCommand
			with ComposableCommand[Extension]
			with AutowiringTransactionalComponent
			with AutowiringUserLookupComponent
			with PubliclyVisiblePermissions
			with Unaudited
	}
}
