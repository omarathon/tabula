package uk.ac.warwick.courses.data.model

import org.joda.time.DateTime
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import javax.persistence.Column
import org.hibernate.annotations.Type
import javax.persistence.Id

// need to manage access to the JSON map
class AuditEvent {
	var id:String =_
	var eventId:String =_
	var eventDate:DateTime =_
	var eventType:String =_
	var eventStage:String =_
	
	var userId:String =_
	
	var masqueradeUserId:String =_
	
	//todo convert to/from json
	var data:String =_
	//var data:Map[String,Any] =_
} 