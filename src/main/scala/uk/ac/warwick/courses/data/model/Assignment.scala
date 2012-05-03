package uk.ac.warwick.courses.data.model
import uk.ac.warwick.courses.JavaImports._
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
import org.hibernate.annotations._
import scala.collection.JavaConversions._
import uk.ac.warwick.courses.ToString
import org.joda.time.Interval

object Assignment {
	val defaultCommentFieldName = "pretext"
	val defaultUploadName = "upload"
		
	final val NotDeletedFilter = "notDeleted"
		
	final val MaximumFileAttachments = 50
}

/**
 * Represents an assignment within a module, occurring at a certain time.
 * 
 * Notes about the notDeleted filter:
 *   filters don't run on session.get() but getById will check for you.
 *   queries will only include it if it's the entity after "from" and not
 *     some other secondary entity joined on. It's usually possible to flip the
 *     query around to make this work.
 */
@FilterDef(name=Assignment.NotDeletedFilter, defaultCondition="deleted = 0")
@Filter(name=Assignment.NotDeletedFilter)
@Entity @AccessType("field")
class Assignment() extends GeneratedId with Viewable with CanBeDeleted with ToString {
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
	@BeanProperty var collectSubmissions:Boolean = false
	@BeanProperty var restrictSubmissions:Boolean = false
	@BeanProperty var allowLateSubmissions:Boolean = true
	@BeanProperty var allowResubmission:Boolean = false
	
	/**
	 * Returns whether we're between the opening and closing dates
	 */
	def isBetweenDates(now:DateTime = new DateTime) =
		isOpened(now) && !isClosed(now)
	
	def isOpened(now:DateTime) = now.isAfter(openDate)
	def isOpened():Boolean = isOpened(new DateTime)
	/**
	 * Whether it's after the close date. Depending on the assignment
	 * we might still be allowing submissions, though.
	 */
	def isClosed(now:DateTime) = now.isAfter(closeDate)
	def isClosed():Boolean = isClosed(new DateTime)
	
	/**
	 * Calculates whether we could submit to this assignment.
	 */
	def submittable = active && collectSubmissions && isOpened() && (allowLateSubmissions || !isClosed())

	/**
	 * Calculates whether we could re-submit to this assignment (assuming that the current
	 * student has already submitted).
	 */
	def resubmittable = submittable && allowResubmission && !isClosed()
	
	@ManyToOne
	@JoinColumn(name="module_id")
	@BeanProperty var module:Module =_
	
	@ManyToOne
	@JoinColumn(name="upstream_id")
	@BeanProperty var upstreamAssignment:UpstreamAssignment =_
	
	@OneToMany(mappedBy="assignment", fetch=FetchType.LAZY, cascade=Array(CascadeType.ALL))
	@OrderBy("submittedDate")
	@BeanProperty var submissions:JList[Submission] = ArrayList()
	
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
		if (fields.exists(_.name == field.name)) throw new IllegalArgumentException("Field with name "+field.name+" already exists")
		field.assignment = this
		field.position = fields.length
		fields.add(field)
	}
	
	def attachmentField: Option[FileField] = findFieldOfType[FileField](Assignment.defaultUploadName)
	def commentField: Option[CommentField] = findFieldOfType[CommentField](Assignment.defaultCommentFieldName)
	
	/**
	 * Find a FormField on the Assignment with the given name.
	 */
	def findField(name:String) : Option[FormField] = fields.find{_.name == name}
	
	/**
	 * Find a FormField on the Assignment with the given name and type.
	 * A field with a matching name but not a matching type is ignored.
	 */
	def findFieldOfType[T <: FormField](name:String)(implicit m:Manifest[T]) : Option[T] = 
		findField(name) match {
			case Some(field) if m.erasure.isInstance(field) => Some(field.asInstanceOf[T])
			case _ => None
		}
	
	/**
	 * Returns a filtered copy of the feedbacks that haven't yet been published.
	 * If the old-style assignment-wide published flag is true, then it
	 * assumes all feedback has already been published.
	 */
	def unreleasedFeedback = feedbacks.filterNot( _.released == true ) // ==true because can be null
	
	def anyReleasedFeedback = feedbacks.find( _.released == true ).isDefined
	
	def addFields(fieldz:FormField*) = for(field<-fieldz) addField(field)
	
	def addFeedback(feedback:Feedback) {
		feedbacks.add(feedback)
		feedback.assignment = this
	}
	
	// Help views decide whether to show a publish button.
	def canPublishFeedback:Boolean = 
			! feedbacks.isEmpty && 
			! unreleasedFeedback.isEmpty && 
			closeDate.isBeforeNow
		
	/**
	 * Report on the submissions and feedbacks, noting
	 * where the lists of students don't match up.
	 */
	def submissionsReport = {
		// Get sets of University IDs
		val feedbackUniIds = feedbacks map {_.universityId} toSet
		val submissionUniIds = submissions map {_.universityId} toSet
		
		// Subtract the sets from each other to obtain discrepencies
		val feedbackOnly = feedbackUniIds &~ submissionUniIds
		val submissionOnly = submissionUniIds &~ feedbackUniIds
		
		SubmissionsReport(this, feedbackOnly, submissionOnly)
	}
	
	def toStringProps = Seq(
		"id" -> id,
		"name" -> name,
		"openDate" -> openDate,
		"closeDate" -> closeDate,
		"module" -> module
	)

}



case class SubmissionsReport(val assignment:Assignment, val feedbackOnly:Set[String], val submissionOnly:Set[String]) {
	def hasProblems = assignment.collectSubmissions && 
		(!feedbackOnly.isEmpty || !submissionOnly.isEmpty)
}