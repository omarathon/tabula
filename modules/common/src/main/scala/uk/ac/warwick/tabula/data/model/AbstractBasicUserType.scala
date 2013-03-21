package uk.ac.warwick.tabula.data.model
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import org.hibernate.`type`.StandardBasicTypes
import org.hibernate.usertype.UserType
import java.{ io => jio }
import java.{ lang => jl }
import org.hibernate.`type`.Type
import org.hibernate.`type`.AbstractStandardBasicType
import org.hibernate.`type`.AbstractSingleColumnStandardBasicType

/**
 * Handles a lot of the junk that isn't necessary if all you want to do is
 * convert between a class and a simple single value.
 *
 * convertTo* methods can assume a non-null input - implement
 * the nullObject and nullValue values to tell it what value to
 * use in the case of null coming from other direction
 */
abstract class AbstractBasicUserType[A <: Object: ClassManifest, B: ClassManifest] extends UserType {

	// Store information about what A is.
	protected val m: ClassManifest[A] = classManifest[A]
	protected val vm: ClassManifest[B] = classManifest[B]

	val basicType: AbstractSingleColumnStandardBasicType[B]
	val nullObject: A // what to use when NULL comes out of the DB
	val nullValue: B // what to put in the DB when saving null
	def convertToObject(input: B): A
	def convertToValue(obj: A): B

	final override def nullSafeGet(resultSet: ResultSet, names: Array[String], owner: Object) = {
		basicType.nullSafeGet(resultSet, names(0)) match {
			case s: Any if s == nullValue => nullObject
			case s: Any if vm.erasure.isInstance(s) => convertToObject(s.asInstanceOf[B])
			case null => nullObject
		}
	}

	final override def nullSafeSet(stmt: PreparedStatement, value: Any, index: Int) =
		basicType.nullSafeSet(stmt, toValue(value), index)

	private final def toValue(value: Any): B = value match {
		case obj: Any if m.erasure.isInstance(value) => convertToValue(value.asInstanceOf[A])
		case null => nullValue
	}

	override def returnedClass = m.erasure

	override def isMutable = false
	override def equals(x: Object, y: Object) = x == y
	override def hashCode(x: Object) = Option(x).getOrElse("").hashCode
	override def deepCopy(x: Object) = x
	override def replace(original: Object, target: Object, owner: Object) = original
	override def disassemble(value: Object) = value.asInstanceOf[jio.Serializable]
	override def assemble(cached: jio.Serializable, owner: Object) = cached.asInstanceOf[AnyRef]
}
