package uk.ac.warwick.courses.data.model
import org.hibernate.annotations.AccessType
import javax.persistence.Entity
import javax.persistence.Lob
import scala.reflect.BeanProperty
import java.io.InputStream
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.CascadeType
import java.sql.Blob
import java.io.File

@Entity @AccessType("field")
class FileAttachment extends GeneratedId {
  
//    @ManyToOne(cascade=Array(CascadeType.ALL))
//	@JoinColumn(name="submission_id")
//	@BeanProperty var submission:Submission = null
	
	//@BeanProperty var feedback:Feedback =_
	
	@BeanProperty var name:String = null
	
	@Lob 
	@BeanProperty var data:Blob = null 
	
	/**
	 * A stream to read the entirety of the data Blob, or null
	 * if there is no Blob.
	 */
	def dataStream = data match {
		case blob:Blob => blob.getBinaryStream(0, blob.length())
		case _ => null
	}
	
	@transient @BeanProperty var uploadedData:InputStream = null
	@transient @BeanProperty var uploadedDataLength:Long = 0
}