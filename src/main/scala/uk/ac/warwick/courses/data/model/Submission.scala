package uk.ac.warwick.courses.data.model
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import javax.validation.constraints.NotNull
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import scala.reflect.BeanProperty
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.CascadeType
import javax.persistence.OneToMany
import javax.persistence.FetchType

@Entity @AccessType("field")
class Submission extends GeneratedId {
  
	@ManyToOne(cascade=Array(CascadeType.PERSIST,CascadeType.MERGE))
	@JoinColumn(name="assignment_id")
	@BeanProperty var assignment:Assignment = _
  
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var date:DateTime =_
	
	@NotNull
	@BeanProperty var userId:String =_
	
	/**
	 * It isn't essential to record University ID as their user ID
	 * will identify them, but a lot of processes require the university
	 * number as the way of identifying a person and it'd be expensive
	 * to go and fetch the value every time. Also if we're keeping
	 * records for a while, the ITS account can be expunged so we'd lose
	 * it entirely.
	 */
	@NotNull
	@BeanProperty var universityId:String =_
	
	@OneToMany(mappedBy="submission", fetch=FetchType.LAZY)
	@BeanProperty var attachments:java.util.Set[FileAttachment] =_
  
}