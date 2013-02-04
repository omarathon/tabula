package uk.ac.warwick.tabula.data.convert

import org.springframework.core.convert.converter.Converter

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User

class UserCodeConverter extends Converter[String, User] {
	
	var userLookup = Wire.auto[UserLookupService]
  	
	override def convert(userId: String) = userLookup.getUserByUserId(userId)

}