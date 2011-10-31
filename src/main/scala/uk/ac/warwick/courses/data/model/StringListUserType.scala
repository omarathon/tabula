package uk.ac.warwick.courses.data.model

import org.hibernate.usertype.UserType
import java.io.Serializable
import java.sql.ResultSet
import java.sql.Types
import org.hibernate.Hibernate
import java.sql.PreparedStatement
import org.hibernate.`type`.StandardBasicTypes

class StringListUserType extends UserType {

	val separator = ","
  
	override def nullSafeGet(resultSet:ResultSet, names:Array[String], owner:Object) = 
	  	split(StandardBasicTypes.STRING.nullSafeGet(resultSet, names(0)))
	
	override def nullSafeSet(stmt:PreparedStatement, value:Object, index:Int) =
		StandardBasicTypes.STRING.nullSafeSet(stmt, join(value), index)
			
	def split(input:String) = input match {
	  case string:String => string.split(separator) 
	  case _ => null
	}
	
	def join(list:Object) = list match {
	  case list:Seq[_] => list.mkString(separator)
	  case _ => null
	}
  
	override def returnedClass = classOf[Seq[String]]
	override def sqlTypes = Array(Types.VARCHAR)
	override def isMutable = false
	override def equals(x:Object, y:Object) = if (x==null) false else x.equals(y)
	override def hashCode(x:Object) = Option(x).getOrElse("").hashCode
	override def deepCopy(x:Object) = x
	override def replace(original:Object, target:Object, owner:Object) = original
	override def disassemble(value:Object) = value.asInstanceOf[Serializable]
	override def assemble(cached:Serializable, owner:Object) = cached

}