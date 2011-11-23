package uk.ac.warwick.courses.data.model
import scala.reflect._
import javax.persistence.Id
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.AccessType
import javax.persistence.OneToMany
import org.joda.time.DateTime
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.CascadeType
import org.hibernate.annotations.Type
import javax.persistence.FetchType
import javax.persistence.OrderBy
import javax.persistence.Column

@Entity @AccessType("field")
class Assignment extends GeneratedId {
	def this(_module:Module) {
	  this()
	  this.module = _module
	}
	
	var academicYear:Int = _
	
	@Type(`type`="uk.ac.warwick.courses.data.model.StringListUserType")
	var fileExtensions:Seq[String] = _
	
	def setAllFileTypesAllowed = { fileExtensions = Nil } 
	
//	@Column("fileextensions")
//	private var _allowedFileExtensions:String = "*"
//	def allowedFileExtensions = _allowedFileExtensions.split(",")
//	def getAllowedFileExtensions = allowedFileExtensions
//	def allowedFileExtensions_=(types:Seq[String]) = { _allowedFileExtensions = types.mkString(",") }
  
	@BeanProperty var attachmentLimit:Int = 1
	
	@BeanProperty var name:String =_
	@BeanProperty var active:Boolean =_
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var openDate:DateTime =_
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var closeDate:DateTime =_
	
	/**
	 * Returns whether we're between the opening and closing dates
	 */
	def isBetweenDates(now:DateTime = new DateTime) =
		now.isAfter(openDate) && now.isBefore(closeDate)
	
	@ManyToOne(cascade=Array(CascadeType.PERSIST,CascadeType.MERGE))
	@JoinColumn(name="module_id")
	@BeanProperty var module:Module =_
	
	@OneToMany(mappedBy="assignment", fetch=FetchType.LAZY)
	@OrderBy("submittedDate")
	@BeanProperty var submissions:java.util.Set[Submission] =_
	
}

