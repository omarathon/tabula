package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.services.{ProfileService, ProfileServiceComponent}


object RemoveAgedApplicantsCommand {
	def apply() =
		new RemoveAgedApplicantsCommandInternal
}

class RemoveAgedApplicantsCommandInternal extends CommandInternal[Unit] {

	val profileService: ProfileService = Wire[ProfileService]
	val memberDao: MemberDao = Wire[MemberDao]

	override protected def applyInternal(): Unit = {

		memberDao.getMissingSince(???)

		???
	}
}

