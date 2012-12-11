package uk.ac.warwick.tabula.data.convert

import org.springframework.core.convert.converter.Converter
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ProfileService

class MemberUniversityIdConverter extends Converter[String, Member] {
  
	var service = Wire.auto[ProfileService]
	
	override def convert(universityId: String) = service.getMemberByUniversityId(universityId).getOrElse(throw new IllegalArgumentException)

}