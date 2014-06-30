package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.attendance.MonitoringCheckpoint
import uk.ac.warwick.tabula.services.UserLookupComponent
import uk.ac.warwick.tabula.helpers.{FoundUser, DateBuilder}

trait CheckpointUpdatedDescription extends UserLookupComponent {

	def describeCheckpoint(checkpoint: MonitoringCheckpoint) = {
		val userString = userLookup.getUserByUserId(checkpoint.updatedBy) match {
			case FoundUser(user) => s"by ${user.getFullName}, "
			case _ => ""
		}

		s"Recorded ${userString}${DateBuilder.format(checkpoint.updatedDate)}"
	}

}
