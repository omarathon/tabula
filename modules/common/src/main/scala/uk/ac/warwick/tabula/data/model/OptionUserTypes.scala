package uk.ac.warwick.tabula.data.model

import java.sql.Types
import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.JavaImports._

class OptionStringUserType extends AbstractBasicUserType[Option[String], String] {
	def convertToObject(input: String) = Option(input)
	def convertToValue(obj: Option[String]) = obj.orNull
	def sqlTypes = Array(Types.VARCHAR)
	val basicType = StandardBasicTypes.STRING
	val nullValue = null
	val nullObject = None
}

class OptionIntegerUserType extends AbstractBasicUserType[Option[Int], JInteger] {
	def convertToObject(input: JInteger) = Option(input)
	def convertToValue(obj: Option[Int]) = obj.map { new JInteger(_) }.orNull
	def sqlTypes = Array(Types.INTEGER)
	val basicType = StandardBasicTypes.INTEGER
	val nullValue = null
	val nullObject = None
}

class OptionBooleanUserType extends AbstractBasicUserType[Option[Boolean], JBoolean] {
	def convertToObject(input: JBoolean) = Option(input)
	def convertToValue(obj: Option[Boolean]) = obj.map { new JBoolean(_) }.orNull
	def sqlTypes = Array(Types.BOOLEAN)
	val basicType = StandardBasicTypes.BOOLEAN
	val nullValue = null
	val nullObject = None
}