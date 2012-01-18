package uk.ac.warwick.courses.data.model
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.sql.Blob
import scala.reflect.BeanProperty
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Repository
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.Lob
import javax.persistence.ManyToOne
import uk.ac.warwick.courses.data.FileDao
import javax.persistence.FetchType

@Configurable
@Entity @AccessType("field")
class FileAttachment extends GeneratedId {
	
	@transient @Autowired var fileDao:FileDao =_
	
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
	@Column(name="data", updatable=true, nullable=true)
	@BeanProperty var blob:Blob = null 
	
	@transient private var _file:File = null
	def file = {
		if (_file == null) _file = fileDao.getData(id).orNull
		_file
	}
		
	@BeanProperty var name:String = _
			
	def this(n:String) { 
		this()
		name = n 
	}
	
	def length = blob match {
		case blob:Blob => blob.length
		case _ => file match {
			case file:File => file.length()
			case _ => 0
		}
	}
	
	/**
	 * A stream to read the entirety of the data Blob, or null
	 * if there is no Blob.
	 */
	def dataStream = blob match {
		case blob:Blob => blob.getBinaryStream
		case _ => file match {
			case file:File => new FileInputStream(file)
			case _ => null
		}
	}
	
	@deprecated
	def hasBlob = blob != null
	def hasData = hasBlob || file != null
	
	@transient @BeanProperty var uploadedData:InputStream = null
	@transient @BeanProperty var uploadedDataLength:Long = 0
	
}
