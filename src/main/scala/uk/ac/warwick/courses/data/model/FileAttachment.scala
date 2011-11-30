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
import org.joda.time.DateTime
import org.hibernate.annotations.Type

@Entity @AccessType("field")
class FileAttachment extends GeneratedId {
	
	// optional link to a Submission
    @ManyToOne(cascade=Array(CascadeType.ALL))
	@JoinColumn(name="submission_id")
	@BeanProperty var submission:Submission = null
	/*
	// optional link to some Feedback
	@ManyToOne(cascade=Array(CascadeType.ALL))
	@JoinColumn(name="feedback_id")
	@BeanProperty var feedback:Feedback =_*/
	
	@BeanProperty var temporary:Boolean = true

	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var dateUploaded:DateTime = new DateTime
	
	@Lob 
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