package uk.ac.warwick.tabula.data.model.groups

import java.sql.Types

import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.data.model.AbstractBasicUserType

sealed abstract class Location extends Serializable {
	def name: String

	override def toString = name
}

@SerialVersionUID(372489712389245l) case class NamedLocation(val name: String) extends Location
@SerialVersionUID(372489712389246l) case class MapLocation(val name: String, val locationId: String) extends Location

object Location {
	def fromDatabase(value: String): Location =
		value.split("\\|", 2) match {
			case Array(name, locationId) => MapLocation(name, locationId)
			case Array(name) => NamedLocation(name)
		}

	def toDatabase(location: Location) =
		location match {
			case NamedLocation(name) => name
			case MapLocation(name, locationId) => s"${name}|${locationId}"
		}
}

class LocationUserType extends AbstractBasicUserType[Location, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(value: String) = Location.fromDatabase(value)
	override def convertToValue(location: Location) = Location.toDatabase(location)

}