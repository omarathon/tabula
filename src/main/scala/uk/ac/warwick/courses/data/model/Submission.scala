package uk.ac.warwick.courses.data.model

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Configurable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.validation.constraints.NotNull
import uk.ac.warwick.courses.JavaImports.JSet
import uk.ac.warwick.courses.actions.Deleteable
import javax.persistence.FetchType._
import javax.persistence.CascadeType._
import scala.collection.mutable
import java.util.HashSet

@Entity @AccessType("field")
class Submission extends GeneratedId with Deleteable {
  
	def this(universityId:String=null) {
		this()
		this.universityId = universityId
	}
	
	def isLate = submittedDate != null && assignment.closeDate.isBefore(submittedDate)
	
	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE))
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

	@OneToMany(mappedBy="submission", cascade=Array(ALL))
	@BeanProperty var values:JSet[SavedSubmissionValue] = new HashSet
	
	def valuesWithAttachments = values.filter(_.hasAttachments)
	
	def allAttachments = valuesWithAttachments.toSeq flatMap { _.attachments }
}

/**
 * Stores a value submitted for a single assignment field. It has
 * a few different fields to handle holding various types of item.
 */
@Entity(name="SubmissionValue") @AccessType("field")
class SavedSubmissionValue extends GeneratedId {
	
	@ManyToOne(fetch=LAZY)
	@JoinColumn(name="submission_id")
	@BeanProperty var submission:Submission =_
	
	// matches with assignment field name
	@BeanProperty var name:String =_
	
	/**
	 * Optional, only for file fields
	 */
	@OneToMany(mappedBy="submissionValue", fetch=LAZY)
	@BeanProperty var attachments:JSet[FileAttachment] =_
	
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