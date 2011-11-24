package uk.ac.warwick.courses.data.model
import scala.reflect._
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OrderBy
import uk.ac.warwick.courses.actions._
import org.joda.time.DateTimeConstants._
import javax.persistence.FetchType
import javax.persistence.CascadeType
import uk.ac.warwick.courses.data.model.forms.FormField
import org.hibernate.annotations.IndexColumn

@Entity @AccessType("field")
class Assignment extends GeneratedId with Viewable {
	def this(_module:Module) {
	  this()
	  this.module = _module
	}
	
	var academicYear:Int = {
		val now = new DateTime
		if (now.getMonthOfYear() >= AUGUST) {
			now.getYear()
		} else {
			now.getYear() - 1
		}
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
	
	/**
	 * Returns whether we're between the opening and closing dates
	 */
	def isBetweenDates(now:DateTime = new DateTime) =
		now.isAfter(openDate) && now.isBefore(closeDate)
	
	def submittable = active && isBetweenDates()
		
	@ManyToOne(cascade=Array(CascadeType.PERSIST,CascadeType.MERGE))
	@JoinColumn(name="module_id")
	@BeanProperty var module:Module =_
	
	@OneToMany(mappedBy="assignment", fetch=FetchType.LAZY)
	@OrderBy("submittedDate")
	@BeanProperty var submissions:java.util.Set[Submission] =_
	
	@OneToMany(mappedBy="assignment", fetch=FetchType.LAZY)
	@IndexColumn(name="position")
	@BeanProperty var fields:java.util.List[FormField] =_
}

