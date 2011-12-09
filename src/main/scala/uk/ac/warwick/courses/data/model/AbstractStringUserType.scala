package uk.ac.warwick.courses.data.model
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

import org.hibernate.`type`.StandardBasicTypes
import org.hibernate.usertype.UserType

import java.{io => jio}

/**
 * Handles a lot of the junk that isn't necessary if all you want to do is
 * convert between a class and a String. Just implement convertToObject
 * and convertToString. Null values are handled so you can assume the value passed
 * in to the methods you override are 
 */
abstract class AbstractStringUserType[T<:Object:ClassManifest] extends UserType {

	// Store information about what T is.
	private val m:ClassManifest[T] = implicitly[ClassManifest[T]]
	
	final override def nullSafeGet(resultSet:ResultSet, names:Array[String], owner:Object) = { 
	  	StandardBasicTypes.STRING.nullSafeGet(resultSet, names(0)) match {
	  		case s:String => convertToObject(s)
	  		case null => null
		}
	}
	
	final override def nullSafeSet(stmt:PreparedStatement, value:Object, index:Int) =
		StandardBasicTypes.STRING.nullSafeSet(stmt, doSet(value), index)
		
	def doSet(value:Any) =
		value match {
			case obj:Any if m.erasure.isInstance(value) => convertToString(value.asInstanceOf[T])
			case null => null 
		}
	
	
			
	def convertToObject(input:String) : T
	def convertToString(obj:T) : String
	
	override def returnedClass = classOf[Seq[String]]
	override def sqlTypes = Array(Types.VARCHAR)
	override def isMutable = false
	override def equals(x:Object, y:Object) = if (x==null) false else x.equals(y)
	override def hashCode(x:Object) = Option(x).getOrElse("").hashCode
	override def deepCopy(x:Object) = x
	override def replace(original:Object, target:Object, owner:Object) = original
	override def disassemble(value:Object) = value.asInstanceOf[jio.Serializable]
	override def assemble(cached:jio.Serializable, owner:Object) = cached
}