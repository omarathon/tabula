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

class SearchProfilesCommand extends Command[List[Member]] with ReadOnly {
	import SearchProfilesCommand._
	
	final val userTypes: Set[MemberUserType] = Set(Student)
	
	var service = Wire.auto[ProfileService]
	
	@NotEmpty(message = "{NotEmpty.profiles.searchQuery}")
	@BeanProperty var query: String = _
	
	override def applyInternal() = usercodeMatches ++ universityIdMatches ++ queryMatches
	
	private def singleton(option: Option[Member]) = 
		if (option.isDefined) List(option.get) filter {userTypes contains _.userType}
		else List()
	
	private def usercodeMatches =
		if (!isMaybeUsercode(query)) List()
		else singleton(service.getMemberByUserId(query))
	
	private def universityIdMatches = 
		if (!isMaybeUniversityId(query)) List()
		else singleton(service.getMemberByUniversityId(query))
	
	private def queryMatches = service.findMembersByQuery(query, userTypes)
	
	override def describe(d: Description) = d.property("query" -> query)

}

object SearchProfilesCommand {
	
	private val UniversityIdPattern = """^\d{7,}$"""
	
	private val UsercodePattern = """^[A-Za-z0-9]{5,}$"""
	
	def isMaybeUniversityId(query: String) = query.trim matches UniversityIdPattern
	
	def isMaybeUsercode(query: String) = query.trim matches UsercodePattern
	
}