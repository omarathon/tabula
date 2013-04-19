package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.Promises._

class MemberUniversityIdConverter extends TwoWayConverter[String, Member] {
  
	val service = promise { Wire[ProfileService] }
	
	override def convertRight(universityId: String) = service.get.getMemberByUniversityId(universityId).orNull
	override def convertLeft(member: Member) = (Option(member) map {_.universityId}).orNull

}