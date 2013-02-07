package uk.ac.warwick.tabula.data.model

import org.codehaus.jackson.map.ObjectMapper
import uk.ac.warwick.spring.Wire
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

/** 
 * Stores a Map[String, Any] as JSON and inflates it back out. 
 * &lt;Paul Daniels&gt;Magic!&lt;/Paul Daniels&gt;
  */
class JsonMapUserType extends AbstractBasicUserType[Map[String, Any], String] {
	
	/** Sad face, Hibernate user types are instantiated in a weird way that make dependency injection hard */
	lazy val jsonMapper = Wire.auto[ObjectMapper]
	
	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null
	
	override def convertToObject(string: String) = jsonMapper.readValue(string, classOf[Map[String, Any]])
	override def convertToValue(map: Map[String, Any]) = jsonMapper.writeValueAsString(map)

}