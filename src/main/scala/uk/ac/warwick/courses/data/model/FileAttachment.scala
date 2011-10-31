package uk.ac.warwick.courses.data.model
import org.hibernate.annotations.AccessType
import javax.persistence.Entity
import javax.persistence.Lob
import scala.reflect.BeanProperty
import java.io.InputStream
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.CascadeType

@Entity @AccessType("field")
class FileAttachment extends GeneratedId {
  
    @ManyToOne(cascade=Array(CascadeType.ALL))
	@JoinColumn(name="submission_id")
	@BeanProperty var submission:Submission = null
	
	@BeanProperty var name:String = null
	
	@Lob 
	@BeanProperty var data:InputStream = null
	
}