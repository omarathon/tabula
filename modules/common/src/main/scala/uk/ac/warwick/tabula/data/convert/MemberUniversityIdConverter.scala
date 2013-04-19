package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.system.TwoWayConverter

class MemberUniversityIdConverter extends TwoWayConverter[String, Member] {
  
	var service = Wire.auto[ProfileService]
	
	override def convertRight(universityId: String) = service.getMemberByUniversityId(universityId).orNull
	override def convertLeft(member: Member) = (Option(member) map {_.universityId}).orNull

}