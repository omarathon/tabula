package uk.ac.warwick.courses.data.model
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

import org.hibernate.`type`.StandardBasicTypes
import org.hibernate.usertype.UserType

import java.{io => jio}
import java.{lang => jl}

/**
 * Handles a lot of the junk that isn't necessary if all you want to do is
 * convert between a class and a number.
 */
abstract class AbstractIntegerUserType[T<:Object:ClassManifest] extends UserType {

	// Store information about what T is.
	private val m:ClassManifest[T] = implicitly[ClassManifest[T]]
	
	final override def nullSafeGet(resultSet:ResultSet, names:Array[String], owner:Object) = { 
	  	StandardBasicTypes.INTEGER.nullSafeGet(resultSet, names(0)) match {
	  		case s:jl.Integer => convertToObject(s)
	  		case null => null
		}
	}
	
	final override def nullSafeSet(stmt:PreparedStatement, value:Object, index:Int) =
		StandardBasicTypes.INTEGER.nullSafeSet(stmt, toInteger(value), index)
		
	private def toInteger(value:Any): jl.Integer =
		value match {
			case obj:Any if m.erasure.isInstance(value) => convertToInteger(value.asInstanceOf[T])
			case null => 0 
		}
	
			
	def convertToObject(input:jl.Integer) : T
	def convertToInteger(obj:T) : jl.Integer
	
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