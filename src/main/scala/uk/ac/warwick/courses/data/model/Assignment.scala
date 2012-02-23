package uk.ac.warwick.courses.data.model
import uk.ac.warwick.courses.JavaImports._
import scala.collection.JavaConversions._
import scala.reflect._
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.IndexColumn
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OrderBy
import uk.ac.warwick.courses.actions._
import uk.ac.warwick.courses.data.model.forms._
import uk.ac.warwick.courses.AcademicYear
import uk.ac.warwick.courses.helpers.DateTimeOrdering._
import javax.persistence.FetchType
import javax.persistence.CascadeType
import uk.ac.warwick.courses.helpers.ArrayList

object Assignment {
	val defaultCommentFieldName = "pretext"
	val defaultUploadName = "upload"
}

@Entity @AccessType("field")
class Assignment() extends GeneratedId with Viewable {
	import Assignment._
	
	def this(_module:Module) {
	  this()
	  this.module = _module
	}
	
	@Basic @Type(`type`="uk.ac.warwick.courses.data.model.AcademicYearUserType")
	@Column(nullable=false)
	var academicYear:AcademicYear = AcademicYear.guessByDate(new DateTime())
	
	/**
	 * Before we allow customising of assignments, we just want the basic
	 * fields to allow you to 
	 */
	def addDefaultFields {
		val pretext = new CommentField
		pretext.name = defaultCommentFieldName
		pretext.value = ""
		
		val file = new FileField
		file.name = defaultUploadName
		
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
	@BeanProperty var feedbacks:JList[Feedback] = ArrayList()
	
	def mostRecentFeedbackUpload = feedbacks.maxBy{_.uploadedDate}.uploadedDate
	
	/**
	 * FIXME IndexColumn doesn't work, currently setting position manually. Investigate!
	 */
	@OneToMany(mappedBy="assignment", fetch=FetchType.LAZY, cascade=Array(CascadeType.ALL))
	@IndexColumn(name="position")
	@BeanProperty var fields:JList[FormField] = ArrayList()
	
	def addField(field:FormField) {
		field.assignment = this
		field.position = fields.length
		fields.add(field)
	}
	
	/**
	 * Returns a filtered copy of the feedbacks that haven't yet been published.
	 * If the old-style assignment-wide published flag is true, then it
	 * assumes all feedback has already been published.
	 */
	def unreleasedFeedback = feedbacks.filterNot( _.released == true ) // ==true because can be null
	
	def anyReleasedFeedback = feedbacks.find( _.released == true ).isDefined
			
	def addFields(fields:FormField*) = for(field<-fields) addField(field)
	
	// Help views decide whether to show a publish button.
	def canPublishFeedback:Boolean = 
			! feedbacks.isEmpty && 
			! unreleasedFeedback.isEmpty && 
			closeDate.isBeforeNow
}

