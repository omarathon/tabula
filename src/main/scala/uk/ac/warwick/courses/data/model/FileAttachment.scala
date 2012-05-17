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
import uk.ac.warwick.courses.data.model.forms.SubmissionValue
import scala.util.matching.Regex

@Configurable
@Entity @AccessType("field")
class FileAttachment extends GeneratedId {
	import FileAttachment._
	
	@transient @Autowired var fileDao:FileDao =_
	
	// optional link to a SubmissionValue
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="submission_id")
	@BeanProperty var submissionValue:SavedSubmissionValue = null
	
	// optional link to some Feedback
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="feedback_id")
	@BeanProperty var feedback:Feedback =_
	
	def isAttached = feedback != null || submissionValue != null
	
	@BeanProperty var temporary:Boolean = true

	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var dateUploaded:DateTime = new DateTime
	
	@transient private var _file:File = null
	def file = {
		if (_file == null) _file = fileDao.getData(id).orNull
		_file
	}
	def file_=(f:File) { _file = f }
	
	@Column(name="name")
	private var _name:String =_
	def name = _name
	def getName() = _name
	def setName(n:String) { name = n }
	def name_= (n:String) {
		_name = Option(n).map(sanitisedFilename).orNull
	}
			
	def this(n:String) { 
		this()
		name = n 
	}
	
	def length:Option[Long] = Option(file) map {_.length}
	
	
	/**
	 * A stream to read the entirety of the data Blob, or null
	 * if there is no Blob.
	 */
	def dataStream = Option(file) map { new FileInputStream(_) } orNull
		
	
	
	def hasData = file != null
	
	@transient @BeanProperty var uploadedData:InputStream = null
	@transient @BeanProperty var uploadedDataLength:Long = 0
	
}

object FileAttachment {
	
	private val BadWindowsCharacters = new Regex("""[<\\"|:*/>?]""")
	
	def sanitisedFilename(filename:String) = BadWindowsCharacters.replaceAllIn(filename.trim, "")
	
}
