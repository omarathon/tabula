package uk.ac.warwick.courses.data.model

import org.joda.time.DateTime
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import javax.persistence.Column
import org.hibernate.annotations.Type
import javax.persistence.Id

// need to manage access to the JSON map
case class AuditEvent(
	var id:String = null,
	var eventId:String = null,
	var eventDate:DateTime = null,
	var eventType:String = null,
	var eventStage:String = null,
	
	var userId:String = null,
	
	var masqueradeUserId:String = null,
	
	//todo convert to/from json
	var data:String = null
	//var data:Map[String,Any] =_
)