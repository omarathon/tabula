package uk.ac.warwick.courses.data.model
import javax.persistence.Entity
import java.util.{List=>JList}
import org.hibernate.annotations.AccessType
import org.joda.time.DateTime
import collection.JavaConversions._

@Entity @AccessType("field")
class Feedback extends GeneratedId {
	var uploaderId:String =_
	var uploadedDate:DateTime = new DateTime
	
	//@OneToMany(mappedBy="module", fetch=FetchType.LAZY, cascade=Array(CascadeType.ALL))
	@transient var attachments:JList[FileAttachment] = List()
}