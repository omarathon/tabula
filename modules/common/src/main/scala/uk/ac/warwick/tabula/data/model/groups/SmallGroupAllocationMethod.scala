package uk.ac.warwick.tabula.data.model.groups

import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class SmallGroupAllocationMethod(val dbValue: String)

object SmallGroupAllocationMethod {
	case object Manual extends SmallGroupAllocationMethod("Manual")
	case object StudentSignUp extends SmallGroupAllocationMethod("StudentSignUp")
	case object Random extends SmallGroupAllocationMethod("Random")

	// lame manual collection. Keep in sync with the case objects above
	val members = Seq(Manual, StudentSignUp, Random)

	def fromDatabase(dbValue: String) =
		if (dbValue == null) null
		else members.find{_.dbValue == dbValue} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}
}

class SmallGroupAllocationMethodUserType extends AbstractBasicUserType[SmallGroupAllocationMethod, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = SmallGroupAllocationMethod.fromDatabase(string)
	override def convertToValue(method: SmallGroupAllocationMethod) = method.dbValue
}