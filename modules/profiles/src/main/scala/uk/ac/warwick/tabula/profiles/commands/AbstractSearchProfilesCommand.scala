package uk.ac.warwick.tabula.profiles.commands


import org.hibernate.validator.constraints.NotEmpty

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.data.model.MemberUserType._

abstract class AbstractSearchProfilesCommand(val user: CurrentUser, firstUserType: MemberUserType, otherUserTypes: MemberUserType*)
	extends Command[Seq[Member]]
		with ReadOnly
		with Unaudited {
	import AbstractSearchProfilesCommand._

	PermissionCheck(Permissions.Profiles.Search)

	final val userTypes = Set(firstUserType) ++ otherUserTypes

	var profileService = Wire.auto[ProfileService]
	var securityService = Wire.auto[SecurityService]
	var moduleService = Wire.auto[ModuleAndDepartmentService]

	@NotEmpty(message = "{NotEmpty.profiles.searchQuery}")
	var query: String = _

	def validQuery =
		(query.trim().length >= MinimumQueryLength) &&
		(query.split("""\s+""").find{_.length >= MinimumTermLength}.isDefined)

	private def singletonByUserType(option: Option[Member]) = option match {
		case Some(member) => Seq(member) filter {userTypes contains _.userType}
		case _ => Seq()
	}

	private def canRead(member: Member) = securityService.can(user, Permissions.Profiles.Read.Core, member)

	def usercodeMatches =
		if (!isMaybeUsercode(query)) Seq()
		else profileService.getAllMembersWithUserId(query).filter {m => userTypes.contains(m.userType)}.filter(canRead)

	def universityIdMatches =
		if (!isMaybeUniversityId(query)) Seq()
		else singletonByUserType(profileService.getMemberByUniversityId(query)) filter canRead

	override def describe(d: Description) = d.property("query" -> query)

}

object AbstractSearchProfilesCommand {

	/** The minimum length of the whole query */
	val MinimumQueryLength = 3

	/** The minimum length of at least one term in the query, avoids searches for "m m m" getting through */
	val MinimumTermLength = 2

	private val UniversityIdPattern = """^\d{7,}$"""

	private val UsercodePattern = """^[A-Za-z0-9]{5,}$"""

	def isMaybeUniversityId(query: String) = query.trim matches UniversityIdPattern

	def isMaybeUsercode(query: String) = query.trim matches UsercodePattern

}
