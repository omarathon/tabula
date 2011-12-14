package uk.ac.warwick.courses.data.model
import java.io.InputStream
import java.sql.Blob
import scala.reflect.BeanProperty
import org.hibernate.annotations.Type
import org.hibernate.annotations.AccessType
import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.Lob
import javax.persistence.ManyToOne
import javax.persistence._

@Entity @AccessType("field")
class FileAttachment extends GeneratedId {
	
	// optional link to a Submission
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="submission_id")
	@BeanProperty var submission:Submission = null
	
	// optional link to some Feedback
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="feedback_id")
	@BeanProperty var feedback:Feedback =_
	
	def isAttached = feedback != null || submission != null
	
	@BeanProperty var temporary:Boolean = true

	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var dateUploaded:DateTime = new DateTime
	
	@Lob @Basic(fetch=FetchType.LAZY)
	@Column(updatable=false, nullable=false)
	@BeanProperty var data:Blob = null 
	
	@BeanProperty var name:String = _
			
	def this(n:String) { 
		this()
		name = n 
	}
	
	def length = data match {
		case blob:Blob => blob.length
		case _ => 0
	}
	
	/**
	 * A stream to read the entirety of the data Blob, or null
	 * if there is no Blob.
	 */
	def dataStream = data match {
		case blob:Blob => blob.getBinaryStream
		case _ => null
	}
	
	@transient @BeanProperty var uploadedData:InputStream = null
	@transient @BeanProperty var uploadedDataLength:Long = 0
}
