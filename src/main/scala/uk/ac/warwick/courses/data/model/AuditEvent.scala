package uk.ac.warwick.courses.data.model

import org.joda.time.DateTime
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import javax.persistence.Column
import org.hibernate.annotations.Type
import javax.persistence.Id

// TODO use this class for querying auditevent.
// need to manage access to the JSON map
class AuditEvent {
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	var eventDate:DateTime =_
	var eventType:String =_
	var eventStage:String =_
	
	@Column(name="user_id")
	var userId:String =_
	
	@Column(name="masquerade_user_id")
	var masqueradeUserId:String =_
	
	//todo convert to/from json
	var data:String =_
	//var data:Map[String,Any] =_
} 