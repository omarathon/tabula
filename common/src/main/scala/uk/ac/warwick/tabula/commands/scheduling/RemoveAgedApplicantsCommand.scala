package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.services.{ProfileService}


object RemoveAgedApplicantsCommand {
	def apply() =
		new RemoveAgedApplicantsCommandInternal
}

class RemoveAgedApplicantsCommandInternal extends CommandInternal[Unit] {

	val profileService: ProfileService = Wire[ProfileService]
	val memberDao: MemberDao = Wire[MemberDao]

	override protected def applyInternal(): Unit = {

		memberDao.getMissingSince(
			from = DateTime.now().minusYears(1),
			memberUserType = MemberUserType.Applicant
		).flatMap { universityId =>
			memberDao.getByUniversityId(
				universityId = universityId,
				disableFilter = true
			)
		}.foreach(memberDao.delete)
	}
}

