package uk.ac.warwick.tabula.data.model

import scala.reflect.BeanProperty
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class MemberUserType(val dbValue: String, @BeanProperty val description: String)
case object Student extends MemberUserType("S", "Student")
case object Staff extends MemberUserType("N", "Staff")
case object Emeritus extends MemberUserType("A", "Emeritus Academic")
case object Other extends MemberUserType("O", "Other")

object MemberUserType {
	def fromCode(code: String) = code match {
	  	case Student.dbValue => Student
	  	case Staff.dbValue => Staff
	  	case Emeritus.dbValue => Emeritus
	  	case Other.dbValue => Other
	  	case null => null
	  	case _ => throw new IllegalArgumentException("Unexpected value: " + code)
	}
}

// Hmm, badly named.
class MemberUserTypeUserType extends AbstractBasicUserType[MemberUserType, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = MemberUserType.fromCode(string)
	
	override def convertToValue(MemberUserType: MemberUserType) = MemberUserType.dbValue

}