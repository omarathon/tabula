package uk.ac.warwick.tabula.attendance.commands.old

import uk.ac.warwick.tabula.data.model.attendance.MonitoringCheckpoint
import uk.ac.warwick.tabula.helpers.{DateBuilder, FoundUser}
import uk.ac.warwick.tabula.services.UserLookupComponent

trait CheckpointUpdatedDescription extends UserLookupComponent {

	def describeCheckpoint(checkpoint: MonitoringCheckpoint) = {
		val userString = userLookup.getUserByUserId(checkpoint.updatedBy) match {
			case FoundUser(user) => s"by ${user.getFullName}, "
			case _ => ""
		}

		s"Recorded ${userString}${DateBuilder.format(checkpoint.updatedDate)}"
	}

}
