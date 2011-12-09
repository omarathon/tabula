package uk.ac.warwick.courses.data.model

import org.hibernate.usertype.UserType
import java.io.Serializable
import java.sql.ResultSet
import java.sql.Types
import org.hibernate.Hibernate
import java.sql.PreparedStatement
import org.hibernate.`type`.StandardBasicTypes

/**
 * For storing comma-separated strings in Hibernate.
 * 
 * Doesn't handle values with commas in them so this is not appropriate
 * for user-inputted data.
 */
class StringListUserType extends AbstractStringUserType[Seq[String]] {

	val separator = ","
  
	override def convertToObject(string:String) = string.split(separator) 
	override def convertToString(list:Seq[String]) = list.mkString(separator)
  
}