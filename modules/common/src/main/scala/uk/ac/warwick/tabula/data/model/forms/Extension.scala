package uk.ac.warwick.tabula.data.model.forms

import scala.collection.JavaConversions._
import org.hibernate.annotations.{BatchSize, Type, AccessType}
import org.joda.time.DateTime
import javax.persistence._
import javax.persistence.CascadeType._
import javax.persistence.FetchType._
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{Feedback, FileAttachment, Assignment, GeneratedId}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl
import uk.ac.warwick.userlookup.User

@Entity @AccessType("field")
class Extension extends GeneratedId with PermissionsTarget {

	def this(universityId:String=null) {
		this()
		this.universityId = universityId
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="assignment_id")
	var assignment:Assignment = _
	
	def permissionsParents = Option(assignment).toStream

	@NotNull
	var userId:String =_

	@NotNull
	var universityId:String =_
	
	def isForUser(user: User): Boolean = isForUser(user.getWarwickId, user.getUserId)
	def isForUser(theUniversityId: String, theUsercode: String): Boolean = universityId == theUniversityId || userId == theUsercode

	// TODO should there be a single def that returns the expiry date for approved/manual extensions, and requested expiry date otherwise?
	@Type(`type`="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var requestedExpiryDate:DateTime =_

	@Type(`type`="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var expiryDate:DateTime =_

	var reason:String =_

	@OneToMany(mappedBy="extension", fetch=LAZY, cascade=Array(ALL))
	@BatchSize(size=200)
	var attachments:JSet[FileAttachment] = JSet()

	def nonEmptyAttachments = attachments.toSeq filter(_.hasData)

	def addAttachment(attachment:FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.extension = this
		attachments.add(attachment)
	}

	var approved:Boolean = false
	var rejected:Boolean = false

	@Type(`type`="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var requestedOn:DateTime =_

	@Type(`type`="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var approvedOn:DateTime =_

	var approvalComments:String =_

	// this was not requested by a student. i.e. was manually created by an administrator
	def isManual = requestedOn == null
	def isAwaitingApproval = !isManual && !approved && !rejected

	@transient
	lazy val workingDaysHelper = new WorkingDaysHelperImpl

	// feedback deadline taking the extension into account
	def feedbackDeadline = {
		val localDate = workingDaysHelper.datePlusWorkingDays(expiryDate.toLocalDate, Feedback.PublishDeadlineInWorkingDays)
		localDate.toDateTime(expiryDate)
	}

}