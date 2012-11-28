package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class Gender(val dbValue: String)
case object Male extends Gender("M")
case object Female extends Gender("F")
case object Unspecified extends Gender("N")

object Gender {
	def fromCode(code: String) = code match {
	  	case "M" => Male
	  	case "F" => Female
	  	case "N" => Unspecified
	  	case null => null
	  	case _ => throw new IllegalArgumentException()
	}
}

class GenderUserType extends AbstractBasicUserType[Gender, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = Gender.fromCode(string)
	
	override def convertToValue(gender: Gender) = gender.dbValue

}