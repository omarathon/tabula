package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object RemoveAgedApplicantsCommand {
	def apply() =
		new RemoveAgedApplicantsCommandInternal
			with RemoveAgedApplicantsPermissions
}

class RemoveAgedApplicantsCommandInternal extends CommandInternal[Unit] {

	val profileService: ProfileService = Wire[ProfileService]
	val memberDao: MemberDao = Wire[MemberDao]

	override protected def applyInternal(): Unit = {

		memberDao.getMissingSince(
			from = DateTime.now().minusMonths(2),
			memberUserType = MemberUserType.Applicant
		).flatMap { universityId =>
			memberDao.getByUniversityId(
				universityId = universityId,
				disableFilter = true
			)
		}.foreach(memberDao.delete)
	}
}

trait RemoveAgedApplicantsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}