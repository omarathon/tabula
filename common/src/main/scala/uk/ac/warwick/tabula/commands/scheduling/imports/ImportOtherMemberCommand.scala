package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.tabula.commands.{Description, Unaudited}
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.services.scheduling.MembershipInformation
import uk.ac.warwick.userlookup.User

class ImportOtherMemberCommand(member: MembershipInformation, ssoUser: User)
	extends ImportMemberCommand(member, ssoUser, None)
	with ApplicantProperties with AutowiringProfileServiceComponent
	with Logging with Daoisms with Unaudited {

	this.mobileNumber = member.sitsApplicantInfo.map(_.mobileNumber).orNull
	this.nationality = member.sitsApplicantInfo.map(_.nationality).orNull
	this.secondNationality = member.sitsApplicantInfo.map(_.secondNationality).orNull
	member.sitsApplicantInfo.foreach(info => profileService.getDisability(info.disability).foreach(this.disability = _))

	def applyInternal(): Member = transactional() {
		val memberExisting = memberDao.getByUniversityIdStaleOrFresh(universityId)
		if (memberExisting.collect { case m @ (_: StudentMember | _: StaffMember) => m }.nonEmpty) {
			// Don't override existing students or staff
			return memberExisting.get
		}

		logger.debug("Importing other member " + universityId + " into " + memberExisting)

		val (isTransient, member) = memberExisting match {
			case Some(m) => (false, m)
			case _ if this.userType == MemberUserType.Applicant => (true, new ApplicantMember(universityId))
			case _ => (true, new OtherMember(universityId))
		}

		val commandBean = new BeanWrapperImpl(this)
		val memberBean = new BeanWrapperImpl(member)

		// We intentionally use single pipes rather than double here - we want all statements to be evaluated
		val hasChanged = member match {
			case _ : ApplicantMember => copyMemberProperties(commandBean, memberBean) | copyApplicantProperties(commandBean, memberBean)
			case _ => copyMemberProperties(commandBean, memberBean)
		}

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + member)

			member.lastUpdatedDate = DateTime.now
			memberDao.saveOrUpdate(member)
		}

		member
	}

	private val basicApplicantProperties = Set(
		"nationality", "secondNationality", "mobileNumber", "disability"
	)

	private def copyApplicantProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicApplicantProperties, commandBean, memberBean)

	override def describe(d: Description): Unit = d.property("universityId" -> universityId).property("category" -> this.userType)

	def phoneNumberPermissions = Nil

}