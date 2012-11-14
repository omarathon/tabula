package uk.ac.warwick.tabula.coursework.data.model
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
abstract class AbstractBasicUserType[T <: Object: ClassManifest, V: ClassManifest] extends UserType {

	// Store information about what T is.
	protected val m: ClassManifest[T] = classManifest[T]
	protected val vm: ClassManifest[V] = classManifest[V]

	val basicType: AbstractSingleColumnStandardBasicType[V]
	val nullObject: T // what to use when NULL comes out of the DB
	val nullValue: V // what to put in the DB when saving null
	def convertToObject(input: V): T
	def convertToValue(obj: T): V

	final override def nullSafeGet(resultSet: ResultSet, names: Array[String], owner: Object) = {
		basicType.nullSafeGet(resultSet, names(0)) match {
			case s: Any if vm.erasure.isInstance(s) => convertToObject(s.asInstanceOf[V])
			case null => nullObject
		}
	}

	final override def nullSafeSet(stmt: PreparedStatement, value: Any, index: Int) =
		basicType.nullSafeSet(stmt, toValue(value), index)

	private final def toValue(value: Any): V = value match {
		case obj: Any if m.erasure.isInstance(value) => convertToValue(value.asInstanceOf[T])
		case null => nullValue
	}

	override def returnedClass = m.erasure

	override def isMutable = false
	override def equals(x: Object, y: Object) = if (x == null) false else x.equals(y)
	override def hashCode(x: Object) = Option(x).getOrElse("").hashCode
	override def deepCopy(x: Object) = x
	override def replace(original: Object, target: Object, owner: Object) = original
	override def disassemble(value: Object) = value.asInstanceOf[jio.Serializable]
	override def assemble(cached: jio.Serializable, owner: Object) = cached
}