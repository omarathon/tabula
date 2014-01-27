package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.attendance.MonitoringCheckpoint
import uk.ac.warwick.tabula.services.ProfileServiceComponent
import uk.ac.warwick.tabula.helpers.DateBuilder

trait CheckpointUpdatedDescription extends ProfileServiceComponent {

	def describeCheckpoint(checkpoint: MonitoringCheckpoint) = {
		val userString = profileService.getAllMembersWithUserId(checkpoint.updatedBy).headOption match {
			case Some(member) => member.fullName match{
				case Some(name) => s"by $name,"
				case _ => ""
			}
			case _ => ""
		}

		s"Recorded $userString ${DateBuilder.format(checkpoint.updatedDate)}"
	}

}
