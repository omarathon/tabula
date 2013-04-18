package uk.ac.warwick.tabula.data.model

import com.fasterxml.jackson.databind.ObjectMapper
import uk.ac.warwick.spring.Wire
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types
import uk.ac.warwick.tabula.helpers.Logging

/** 
 * Stores a Map[String, Any] as JSON and inflates it back out. 
 * &lt;Paul Daniels&gt;Magic!&lt;/Paul Daniels&gt;
  */
class JsonMapUserType extends AbstractBasicUserType[Map[String, Any], String] with Logging {
	
	/** Sad face, Hibernate user types are instantiated in a weird way that make dependency injection hard */
	var jsonMapper = Wire.option[ObjectMapper]
	
	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null
	
	override def convertToObject(string: String) = jsonMapper match {
		case Some(jsonMapper) => jsonMapper.readValue(string, classOf[Map[String, Any]])
		case _ => {
			logger.warn("No JSON mapper defined. This should only happen in unit tests!")
			nullObject
		} 
	}
			
	override def convertToValue(map: Map[String, Any]) = jsonMapper match {
		case Some(jsonMapper) => jsonMapper.writeValueAsString(map)
		case _ => {
			logger.warn("No JSON mapper defined. This should only happen in unit tests!")
			nullValue
		} 
	}

}