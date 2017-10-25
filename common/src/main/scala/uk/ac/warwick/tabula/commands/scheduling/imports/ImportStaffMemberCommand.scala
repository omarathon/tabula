package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.tabula.commands.{Description, Unaudited}
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.MembershipInformation
import uk.ac.warwick.userlookup.User

class ImportStaffMemberCommand(info: MembershipInformation, ssoUser: User)
	extends ImportMemberCommand(info, ssoUser, None)
	with Logging with Daoisms with StaffProperties with Unaudited {

	this.teachingStaff = info.member.teachingStaff

	def applyInternal(): Member = transactional() {
		val memberExisting = memberDao.getByUniversityIdStaleOrFresh(universityId)

		logger.debug("Importing staff member " + universityId + " into " + memberExisting)

		val isTransient = memberExisting.nonEmpty

		def newMember: Member = {
			if (this.userType == MemberUserType.Emeritus) new EmeritusMember(universityId)
			else new StaffMember(universityId)
		}

		/* If the member exists and it's type has changed, delete the member and create a fresh one of the correct type - allows
		 * Other, Emeritus => Staff
		 * Other, Staff, => Emeritus
		 * (this.userType will be Emeritus or Staff if this command is being executed)
		 */
		val recreatedMember = memberExisting
			.filter(_.userType != this.userType)
			.collect { case m @ (_: EmeritusMember | _: StaffMember | _:OtherMember) => m }
			.map(m => {
				logger.info(s"Deleting $m while importing $universityId")
				memberDao.delete(m)
				newMember
			})

		val member = recreatedMember.orElse(memberExisting).getOrElse(newMember)

		val commandBean = new BeanWrapperImpl(this)
		val memberBean = new BeanWrapperImpl(member)

		// We intentionally use a single pipe rather than a double pipe here - we want both statements to be evaluated
		val hasChanged = copyMemberProperties(commandBean, memberBean) |
			(member.isInstanceOf[StaffMember] && copyStaffProperties(commandBean, memberBean))

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + member)

			member.lastUpdatedDate = DateTime.now
			memberDao.saveOrUpdate(member)
		}

		member
	}

	private val basicStaffProperties: Set[String] = Set(
		"teachingStaff"
	)

	private def copyStaffProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicStaffProperties, commandBean, memberBean)


	override def describe(d: Description): Unit = d.property("universityId" -> universityId).property("category" -> "staff")


}