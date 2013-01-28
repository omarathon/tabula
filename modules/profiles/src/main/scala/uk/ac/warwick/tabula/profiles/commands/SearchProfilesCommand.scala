package uk.ac.warwick.tabula.profiles.commands

import org.hibernate.validator.constraints.NotEmpty
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.commands.Command
import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.data.model.Student
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.actions.Search
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.CurrentUser

class SearchProfilesCommand(val currentMember: Member, val user: CurrentUser) extends Command[Seq[Member]] with ReadOnly with Unaudited {
	import SearchProfilesCommand._
	
	PermissionsCheck(Search(classOf[Member]))
	
	final val userTypes: Set[MemberUserType] = Set(Student)
	
	var profileService = Wire.auto[ProfileService]
	var securityService = Wire.auto[SecurityService]
	
	@NotEmpty(message = "{NotEmpty.profiles.searchQuery}")
	@BeanProperty var query: String = _
	
	override def applyInternal() =
		if (validQuery) usercodeMatches ++ universityIdMatches ++ queryMatches
		else Seq()
		
	private def validQuery = 
		(query.trim().length > MinimumQueryLength) &&
		(query.split("""\s+""") find {_.length > MinimumTermLength} isDefined)
	
	private def singletonByUserType(option: Option[Member]) = 
		if (option.isDefined) Seq(option.get) filter {userTypes contains _.userType}
		else Seq()
	
	private def usercodeMatches =
		if (!isMaybeUsercode(query)) Seq()
		else singletonByUserType(profileService.getMemberByUserId(query))
	
	private def universityIdMatches = 
		if (!isMaybeUniversityId(query)) Seq()
		else singletonByUserType(profileService.getMemberByUniversityId(query))
	
	private def queryMatches = {
		profileService.findMembersByQuery(query, currentMember.affiliatedDepartments, userTypes, user.god)
	}
	
	override def describe(d: Description) = d.property("query" -> query)

}

object SearchProfilesCommand {
	
	/** The minimum length of the whole query */
	val MinimumQueryLength = 3
	
	/** The minimum length of at least one term in the query, avoids searches for "m m m" getting through */
	val MinimumTermLength = 2
	
	private val UniversityIdPattern = """^\d{7,}$"""
	
	private val UsercodePattern = """^[A-Za-z0-9]{5,}$"""
	
	def isMaybeUniversityId(query: String) = query.trim matches UniversityIdPattern
	
	def isMaybeUsercode(query: String) = query.trim matches UsercodePattern
	
}