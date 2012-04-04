package uk.ac.warwick.courses.data.model

import scala.reflect.BeanProperty
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Configurable
import javax.persistence._
import javax.validation.constraints.NotNull
import uk.ac.warwick.courses.JavaImports.JSet
import uk.ac.warwick.courses.actions.Deleteable
import collection.JavaConversions._

@Entity @AccessType("field")
class Submission extends GeneratedId with Deleteable {
  
	def this(universityId:String) {
		this()
		this.universityId = universityId
	}
	
	@ManyToOne(optional=false, cascade=Array(CascadeType.PERSIST,CascadeType.MERGE))
	@JoinColumn(name="assignment_id")
	@BeanProperty var assignment:Assignment = _
  
	@BeanProperty var submitted:Boolean = false
	
	@Column(name="submitted_date")
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var submittedDate:DateTime =_
	
	@NotNull
	@BeanProperty var userId:String =_
	
	/**
	 * It isn't essential to record University ID as their user ID
	 * will identify them, but a lot of processes require the university
	 * number as the way of identifying a person and it'd be expensive
	 * to go and fetch the value every time. Also if we're keeping
	 * records for a while, the ITS account can be expunged so we'd lose
	 * it entirely.
	 */
	@NotNull
	@BeanProperty var universityId:String =_

	@OneToMany(mappedBy="submission", cascade=Array(CascadeType.ALL))
	@BeanProperty var values:JSet[SavedSubmissionValue] =_
	
	def valuesWithAttachments = values.filter(_.hasAttachments)
	
	def allAttachments = valuesWithAttachments.toSeq flatMap { _.attachments }
}

/**
 * Stores a value submitted for a single assignment field. It has
 * a few different fields to handle holding various types of item.
 */
@Entity(name="SubmissionValue") @AccessType("field")
class SavedSubmissionValue extends GeneratedId {
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="submission_id")
	@BeanProperty var submission:Submission =_
	
	// matches with assignment field name
	@BeanProperty var name:String =_
	
	/**
	 * Optional, only for file fields
	 */
	@OneToMany(mappedBy="submissionValue", fetch=FetchType.LAZY)
	@BeanProperty var attachments:java.util.Set[FileAttachment] =_
	
	def hasAttachments = attachments != null && !attachments.isEmpty
	
	@BeanProperty var value:String =_
}

object SavedSubmissionValue {
	def withAttachments(submission:Submission, name:String, attachments:java.util.Set[FileAttachment]) = {
		val value = new SavedSubmissionValue()
		value.submission = submission
		value.name = name
		value.attachments = attachments
		value
	}
}