package uk.ac.warwick.tabula.data.model.permissions

import org.hibernate.usertype.CompositeUserType
import org.hibernate.`type`.StandardBasicTypes
import org.hibernate.engine.SessionImplementor
import org.hibernate.`type`.Type
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import org.hibernate.HibernateException
import java.sql.ResultSet
import java.sql.PreparedStatement

/** 
 * Essentially this is a catch-all session lookup. We can give it a 
 * department and its ID, or a module, or a student profile, or anything 
 * really - and it should convert to/from its representation as a 
 * PermissionsTarget.
  */
class PermissionsTargetUserType extends CompositeUserType {
	type JSerializable = java.io.Serializable
	
	val propertyNames = Array("type", "id")
	val basicTypes: Array[Type] = Array(StandardBasicTypes.STRING, StandardBasicTypes.STRING)

	val nullValue = null
	val nullObject = null
	
  def assemble(cached: JSerializable, session: SessionImplementor, owner: Object) = cached
  def disassemble(value: Object, session: SessionImplementor) = value.asInstanceOf[JSerializable]
  def deepCopy(value: Object) = value
  
  def equals(x: Object, y: Object) = x == y
  
  def getPropertyNames = propertyNames
  def getPropertyTypes = basicTypes
  
  def getPropertyValue(component: Object, property: Int) = component match {
		case null => null
		case target: PermissionsTarget => if (property == 0) target.getClass.getSimpleName else target.id
	}
	
	def hashCode(x: Object) = x.hashCode
	def isMutable = false
	
	def nullSafeGet(resultSet: ResultSet, names: Array[String], session: SessionImplementor, owner: Object) =
		if (resultSet == null) null
		else {
			val shortName = StandardBasicTypes.STRING.nullSafeGet(resultSet, names(0))
			val id = StandardBasicTypes.STRING.nullSafeGet(resultSet, names(1))
			if (shortName == null || id == null) null
			else session.internalLoad(shortName, id, false, false)
		}
	
	def nullSafeSet(statement: PreparedStatement, value: Object, index: Int, session: SessionImplementor) = value match {
		case null => {
			statement.setNull(index, StandardBasicTypes.STRING.sqlType());
      statement.setNull(index + 1, StandardBasicTypes.STRING.sqlType());
		}
		case target: PermissionsTarget => {
			statement.setString(index, target.getClass.getSimpleName)
			statement.setString(index + 1, target.id)
		}
	}
	
	def replace(original: Object, target: Object, session: SessionImplementor, owner: Object) = original
	def returnedClass = classOf[PermissionsTarget]
	
	def setPropertyValue(component: Object, property: Int, value: Object) =
		throw new UnsupportedOperationException("Immutable")

}