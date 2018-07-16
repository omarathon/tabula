package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.{AutowiringMemberDaoComponent, MemberDaoComponent}
import uk.ac.warwick.tabula.data.model.{ApplicantMember, Member, MemberUserType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User


object RemoveAgedApplicantsCommand {
	def apply() =
		new RemoveAgedApplicantsCommandInternal
			with ComposableCommand[Seq[String]]
			with AutowiringPermissionsServiceComponent
			with AutowiringMemberDaoComponent
			with RemoveAgedApplicantsPermissions
			with RemoveAgedApplicantsDescription
}

class RemoveAgedApplicantsCommandInternal extends CommandInternal[Seq[String]] {
	self: PermissionsServiceComponent with MemberDaoComponent =>

	override protected def applyInternal(): Seq[String] = transactional() {
		val membersToBeRemoved: Seq[Member] = memberDao.getMissingBefore[ApplicantMember](
			from = DateTime.now().minusMonths(2)
		).flatMap { universityId =>
			memberDao.getByUniversityId(
				universityId = universityId,
				disableFilter = true
			)
		}
		val userCodes = membersToBeRemoved.map(_.userId)
		membersToBeRemoved.foreach(memberDao.delete)
		userCodes
	}
}

trait RemoveAgedApplicantsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}

trait RemoveAgedApplicantsDescription extends Describable[Seq[String]] {
	override def describe(d: Description) {}

	override def describeResult(d: Description, result: Seq[String]): Unit = {
		d.users(result.map { uniId =>
			val user = new User
			user.setWarwickId(uniId)
			user
		})
	}
}