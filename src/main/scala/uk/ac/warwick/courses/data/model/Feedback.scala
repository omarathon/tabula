package uk.ac.warwick.courses.data.model
import java.util.{List => JList}
import scala.collection.JavaConversions._
import org.hibernate.annotations.AccessType
import org.joda.time.DateTime
import javax.persistence.Entity
import uk.ac.warwick.courses.helpers.ArrayList
import javax.persistence.Column
import org.hibernate.annotations.Type
import javax.persistence.ManyToOne
import scala.reflect.BeanProperty
import javax.persistence.FetchType
import javax.persistence.OneToMany
import javax.persistence.CascadeType

@Entity @AccessType("field")
class Feedback extends GeneratedId {
	
	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@BeanProperty var assignment:Assignment =_
	
	var uploaderId:String =_
	
	@Column(name="uploaded_date")
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	var uploadedDate:DateTime = new DateTime
	
	var universityId:String =_
	
	@OneToMany(mappedBy="feedback", fetch=FetchType.LAZY)
	var attachments:JList[FileAttachment] = ArrayList()
	
	def addAttachment(attachment:FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.feedback = this
		attachments.add(attachment)
	}
}