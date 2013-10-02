package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.ResultSet
import org.joda.time.DateTime
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.StaffProperties
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.{StaffMember, EmeritusMember}
import uk.ac.warwick.tabula.data.model.MemberUserType

class ImportStaffMemberCommand(member: MembershipInformation, ssoUser: User)
	extends ImportMemberCommand(member, ssoUser, None)
	with Logging with Daoisms with StaffProperties with Unaudited {

	import ImportMemberHelpers._

	// TODO reinstate this, one day
//	this.teachingStaff = rs.getString("teaching_staff") == "Y"

	def applyInternal(): Member = transactional() {
		val memberExisting = memberDao.getByUniversityId(universityId)

		logger.debug("Importing member " + universityId + " into " + memberExisting)

		val isTransient = !memberExisting.isDefined
		val member = memberExisting getOrElse {
			if (this.userType == MemberUserType.Emeritus) new EmeritusMember(universityId)
			else new StaffMember(universityId)
		}

		val commandBean = new BeanWrapperImpl(this)
		val memberBean = new BeanWrapperImpl(member)

		// We intentionally use a single pipe rather than a double pipe here - we want both statements to be evaluated
		val hasChanged = copyMemberProperties(commandBean, memberBean) | copyStaffProperties(commandBean, memberBean)


		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + member)

			member.lastUpdatedDate = DateTime.now
			memberDao.saveOrUpdate(member)
		}

		member
	}

	private val basicStaffProperties: Set[String] = Set(
//		"teachingStaff"
	)

	private def copyStaffProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicStaffProperties, commandBean, memberBean)


	override def describe(d: Description) = d.property("universityId" -> universityId).property("category" -> "staff")


}