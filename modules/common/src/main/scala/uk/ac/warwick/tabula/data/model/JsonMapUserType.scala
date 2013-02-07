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
	
	val json = Wire.auto[ObjectMapper]
	
	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null
	
	override def convertToObject(string: String) = json.readValue(string, classOf[Map[String, Any]])
	override def convertToValue(map: Map[String, Any]) = json.writeValueAsString(map)

}