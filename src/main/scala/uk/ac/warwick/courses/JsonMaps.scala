package uk.ac.warwick.courses

import collection.JavaConverters._
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.Module
import org.codehaus.jackson.map.Module.SetupContext
import org.codehaus.jackson.map.`type`.TypeModifier

trait JsonMaps {
	implicit def jsonableMap(map:Map[_,_]) = new {
		def jsonify = objectMapper.writeValueAsString(map)
	}
	
	def objectMapper = {
		new ObjectMapper()
	}
}