package uk.ac.warwick.courses.data.model
import java.util.{List => JList}

import scala.collection.JavaConversions._

import org.hibernate.annotations.AccessType
import org.joda.time.DateTime

import javax.persistence.Entity


@Entity @AccessType("field")
class Feedback extends GeneratedId {
	var uploaderId:String =_
	var uploadedDate:DateTime = new DateTime
	
	var universityId:String =_
	
	//@OneToMany(mappedBy="module", fetch=FetchType.LAZY, cascade=Array(CascadeType.ALL))
	@transient var attachments:JList[FileAttachment] = List()
}