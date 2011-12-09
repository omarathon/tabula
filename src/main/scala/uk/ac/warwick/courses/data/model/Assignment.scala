package uk.ac.warwick.courses.data.model
import java.util.ArrayList
import java.util.{List => JList}
import scala.collection.JavaConversions._
import scala.reflect._
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.IndexColumn
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OrderBy
import uk.ac.warwick.courses.actions._
import uk.ac.warwick.courses.data.model.forms._
import uk.ac.warwick.courses.AcademicYear
import javax.persistence.FetchType
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Basic

@Entity @AccessType("field")
class Assignment() extends GeneratedId with Viewable {
	def this(_module:Module) {
	  this()
	  this.module = _module
	}
	
	@Basic @Type(`type`="uk.ac.warwick.courses.data.model.AcademicYearUserType")
	@Column(nullable=false)
	var academicYear:AcademicYear = _
	
	def addDefaultFields {
		val pretext = new CommentField
		pretext.name = "pretext"
		pretext.value = ""
		
		val file = new FileField
		file.name = "upload"
		
		addFields(pretext, file)
	}

	
	@Type(`type`="uk.ac.warwick.courses.data.model.StringListUserType")
	var fileExtensions:Seq[String] = _
	
	def setAllFileTypesAllowed { fileExtensions = Nil } 
  
	@BeanProperty var attachmentLimit:Int = 1
	
	@BeanProperty var name:String =_
	@BeanProperty var active:Boolean =_
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var openDate:DateTime =_
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var closeDate:DateTime =_
	
	var collectMarks:Boolean =_
	
	/**
	 * Returns whether we're between the opening and closing dates
	 */
	def isBetweenDates(now:DateTime = new DateTime) =
		now.isAfter(openDate) && now.isBefore(closeDate)
	
	def submittable = active && isBetweenDates()
		
	@ManyToOne
	@JoinColumn(name="module_id")
	@BeanProperty var module:Module =_
	
	@OneToMany(mappedBy="assignment", fetch=FetchType.LAZY, cascade=Array(CascadeType.ALL))
	@OrderBy("submittedDate")
	@BeanProperty var submissions:JList[Submission] =_
	
	@OneToMany(mappedBy="assignment", fetch=FetchType.LAZY, cascade=Array(CascadeType.ALL))
	@BeanProperty var feedbacks:JList[Feedback] =_
	
	/**
	 * FIXME IndexColumn doesn't work, currently setting position manually. Investigate!
	 */
	@OneToMany(mappedBy="assignment", fetch=FetchType.LAZY, cascade=Array(CascadeType.ALL))
	@IndexColumn(name="position")
	@BeanProperty var fields:JList[FormField] = new ArrayList
	
	@BeanProperty var resultsPublished:Boolean = false
	
	def addField(field:FormField) {
		field.assignment = this
		field.position = fields.length
		fields.add(field)
	}
	
	def addFields(fields:FormField*) = for(field<-fields) addField(field)
}

