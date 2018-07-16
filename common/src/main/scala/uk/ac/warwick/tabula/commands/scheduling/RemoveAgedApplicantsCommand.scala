package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.data.{AutowiringMemberDaoComponent, MemberDaoComponent}
import uk.ac.warwick.tabula.data.model.{ApplicantMember, MemberUserType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object RemoveAgedApplicantsCommand {
	def apply() =
		new RemoveAgedApplicantsCommandInternal
			with AutowiringPermissionsServiceComponent
			with AutowiringMemberDaoComponent
			with RemoveAgedApplicantsPermissions
}

class RemoveAgedApplicantsCommandInternal extends CommandInternal[Unit] {
	self: PermissionsServiceComponent with MemberDaoComponent =>

	override protected def applyInternal(): Unit = {
		memberDao.getMissingBefore[ApplicantMember](
			from = DateTime.now().minusMonths(2)
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