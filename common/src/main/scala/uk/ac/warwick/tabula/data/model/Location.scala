package uk.ac.warwick.tabula.data.model

import java.sql.Types

import org.hibernate.`type`.StandardBasicTypes

sealed abstract class Location extends Serializable {
	def name: String
	override def toString: String = name
}

@SerialVersionUID(372489712389245l) case class NamedLocation(name: String) extends Location
@SerialVersionUID(372489712389246l) case class MapLocation(name: String, locationId: String, syllabusPlusName: Option[String] = None) extends Location
@SerialVersionUID(372489712389246l) case class AliasedMapLocation(alias: String, mapLocation: MapLocation) extends Location {
	override def name: String = alias
	def locationId: String = mapLocation.locationId
}

object Location {
	def fromDatabase(value: String): Location =
		value.split("\\|", 4) match {
			case Array(displayName, name, locationId, syllabusPlusName) => AliasedMapLocation(displayName, MapLocation(name, locationId, Some(syllabusPlusName)))
			case Array(name, locationId, syllabusPlusName) => MapLocation(name, locationId, Some(syllabusPlusName))
			case Array(name, locationId) => MapLocation(name, locationId)
			case Array(name) => NamedLocation(name)
		}

	def toDatabase(location: Location): String =
		location match {
			case NamedLocation(name) => name
			case MapLocation(name, locationId, syllabusPlusName) => s"$name|$locationId|${syllabusPlusName.getOrElse("")}"
			case AliasedMapLocation(displayName, mapLocation) => s"$displayName|${toDatabase(mapLocation)}"
		}
}

class LocationUserType extends AbstractBasicUserType[Location, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(value: String): Location = Location.fromDatabase(value)
	override def convertToValue(location: Location): String = Location.toDatabase(location)

}