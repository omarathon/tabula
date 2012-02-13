package uk.ac.warwick.courses.data.model
import uk.ac.warwick.courses.JavaImports._
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
import uk.ac.warwick.courses.actions.Viewable
import uk.ac.warwick.courses.JavaImports._

@Entity @AccessType("field")
class Feedback extends GeneratedId with Viewable {
	
	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@BeanProperty var assignment:Assignment =_
	
	var uploaderId:String =_
	
	@Column(name="uploaded_date")
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	var uploadedDate:DateTime = new DateTime
	
	var universityId:String =_
	
	var released:JBoolean = false
	
	/**
	 * Returns the released flag of this feedback,
	 * OR the parent assignment's value if not set.
	 */
	def checkedReleased:Boolean = Option(released) match {
		case Some(bool) => bool
		case None => assignment.resultsPublished
	}
	
	@OneToMany(mappedBy="feedback", fetch=FetchType.LAZY)
	var attachments:JList[FileAttachment] = ArrayList()
	
	def addAttachment(attachment:FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.feedback = this
		attachments.add(attachment)
	}
}