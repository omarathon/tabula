package uk.ac.warwick.courses.data.model

import scala.collection.JavaConversions.asScalaBuffer
import scala.reflect.BeanProperty
import scala.reflect.Manifest
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.IndexColumn
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Configurable
import Assignment.defaultCommentFieldName
import Assignment.defaultUploadName
import javax.persistence._
import uk.ac.warwick.courses.JavaImports.JList
import uk.ac.warwick.courses.actions.Viewable
import uk.ac.warwick.courses.data.model.forms.CommentField
import uk.ac.warwick.courses.data.model.forms.Extension
import uk.ac.warwick.courses.data.model.forms.FileField
import uk.ac.warwick.courses.data.model.forms.FormField
import uk.ac.warwick.courses.helpers.DateTimeOrdering.orderedDateTime
import uk.ac.warwick.courses.helpers.ArrayList
import uk.ac.warwick.courses.{CurrentUser, AcademicYear, ToString, Features}
import javax.persistence.FetchType._
import javax.persistence.CascadeType._
import uk.ac.warwick.courses.services.{AssignmentService, UserLookupService}
import uk.ac.warwick.userlookup.User
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.Resource
import uk.ac.warwick.courses.JBoolean

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
 * filters don't run on session.get() but getById will check for you.
 * queries will only include it if it's the entity after "from" and not
 * some other secondary entity joined on. It's usually possible to flip the
 * query around to make this work.
 */
@FilterDef(name = Assignment.NotDeletedFilter, defaultCondition = "deleted = 0")
@Filter(name = Assignment.NotDeletedFilter)
@Entity
@AccessType("field")
@Configurable
class Assignment() extends GeneratedId with Viewable with CanBeDeleted with ToString {

	import Assignment._

	@transient
	@Resource(name = "assignmentService") var assignmentService: AssignmentService = _
	@transient
	@Resource(name = "userLookup") var userLookup: UserLookupService = _

	def this(_module: Module) {
		this()
		this.module = _module
	}

	@Basic
	@Type(`type` = "uk.ac.warwick.courses.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime())

	@BeanProperty var occurrence: String = _

	@Type(`type` = "uk.ac.warwick.courses.data.model.StringListUserType")
	@BeanProperty var fileExtensions: Seq[String] = _

	@BeanProperty var attachmentLimit: Int = 1

	@BeanProperty var name: String = _
	@BeanProperty var active: JBoolean = _

	@BeanProperty var archived: JBoolean = false

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var openDate: DateTime = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var closeDate: DateTime = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var createdDate = DateTime.now()

	@BeanProperty var collectMarks:JBoolean = false
	@BeanProperty var collectSubmissions:JBoolean = false
	@BeanProperty var restrictSubmissions:JBoolean = false
	@BeanProperty var allowLateSubmissions:JBoolean = true
	@BeanProperty var allowResubmission:JBoolean = false
	@BeanProperty var displayPlagiarismNotice:JBoolean = false
	
	
	@BeanProperty var allowExtensions:JBoolean = false
	// allow students to request extensions via the app
	
	@BeanProperty var allowExtensionRequests:JBoolean = false

	@ManyToOne
	@JoinColumn(name = "module_id")
	@BeanProperty var module: Module = _

	@ManyToOne
	@JoinColumn(name = "upstream_id")
	@BeanProperty var upstreamAssignment: UpstreamAssignment = _

	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	@OrderBy("submittedDate")
	@BeanProperty var submissions: JList[Submission] = ArrayList()

	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	@BeanProperty var extensions: JList[Extension] = ArrayList()

	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	@BeanProperty var feedbacks: JList[Feedback] = ArrayList()

	/**
	 * FIXME IndexColumn doesn't work, currently setting position manually. Investigate!
	 */
	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	@IndexColumn(name = "position")
	@BeanProperty var fields: JList[FormField] = ArrayList()

	@OneToOne(cascade = Array(CascadeType.ALL))
	@JoinColumn(name = "membersgroup_id")
	@BeanProperty var members: UserGroup = new UserGroup

	def setAllFileTypesAllowed() {
		fileExtensions = Nil
	}

	/**
	 * Before we allow customising of assignments, we just want the basic
	 * fields to allow you to
	 */
	def addDefaultFields() {
		val pretext = new CommentField
		pretext.name = defaultCommentFieldName
		pretext.value = ""

		val file = new FileField
		file.name = defaultUploadName

		addFields(pretext, file)
	}

	/**
	 * Returns whether we're between the opening and closing dates
	 */
	def isBetweenDates(now: DateTime = new DateTime) =
		isOpened(now) && !isClosed(now)

	def isOpened(now: DateTime) = now.isAfter(openDate)

	def isOpened(): Boolean = isOpened(new DateTime)

	/**
	 * Whether it's after the close date. Depending on the assignment
	 * we might still be allowing submissions, though.
	 */
	def isClosed(now: DateTime) = now.isAfter(closeDate)

	def isClosed(): Boolean = isClosed(new DateTime)

	/**
	 * True if the specified user has been granted an extension and that extension has not expired on the specified date
	 */
	def isWithinExtension(userId: String, time: DateTime) =
		extensions.exists(e => e.userId == userId && e.approved && e.expiryDate.isAfter(time))

	/**
	 * True if the specified user has been granted an extension and that extension has not expired now
	 */
	def isWithinExtension(userId: String): Boolean = isWithinExtension(userId, new DateTime)

	/**
	 * retrospectively checks if a submission was late. called by submission.isLate to check against extensions
	 */
	def isLate(submission: Submission) =
		closeDate.isBefore(submission.submittedDate) && !isWithinExtension(submission.userId, submission.submittedDate)

	/**
	* retrospectively checks if a submission was an 'authorised late'
	* called by submission.isAuthorisedLate to check against extensions
	*/
	def isAuthorisedLate(submission:Submission) =
		closeDate.isBefore(submission.submittedDate) && isWithinExtension(submission.userId, submission.submittedDate)

	// returns extension for a specified student
	def findExtension(uniId:String) = extensions.find(_.universityId == uniId)



  def assessmentGroup: Option[UpstreamAssessmentGroup] = {
		if (upstreamAssignment == null || academicYear == null || occurrence == null) {
			None
		} else {
			val template = new UpstreamAssessmentGroup
			template.academicYear = academicYear
			template.assessmentGroup = upstreamAssignment.assessmentGroup
			template.moduleCode = upstreamAssignment.moduleCode
			template.occurrence = occurrence
			assignmentService.getAssessmentGroup(template)
		}
	}

	/**
	 * Calculates whether we could submit to this assignment.
	 */
	def submittable(uniId:String) = active && collectSubmissions && isOpened() && (allowLateSubmissions || !isClosed() || isWithinExtension(uniId))

	/**
	 * Calculates whether we could re-submit to this assignment (assuming that the current
	 * student has already submitted).
	 */
	def resubmittable(uniId:String) = submittable(uniId) && allowResubmission && (!isClosed() || isWithinExtension(uniId))
	
	def mostRecentFeedbackUpload = feedbacks.maxBy{
		_.uploadedDate
	}.uploadedDate
	
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
	def findField(name: String): Option[FormField] = fields.find {
		_.name == name
	}

	/**
	 * Find a FormField on the Assignment with the given name and type.
	 * A field with a matching name but not a matching type is ignored.
	 */
	def findFieldOfType[T <: FormField](name: String)(implicit m: Manifest[T]): Option[T] =
		findField(name) match {
			case Some(field) if m.erasure.isInstance(field) => Some(field.asInstanceOf[T])
			case _ => None
		}

	/**
	 * Returns a filtered copy of the feedbacks that haven't yet been published.
	 * If the old-style assignment-wide published flag is true, then it
	 * assumes all feedback has already been published.
	 */
	def unreleasedFeedback = feedbacks.filterNot(_.released == true) // ==true because can be null

	def anyReleasedFeedback = feedbacks.find(_.released == true).isDefined

	def addFields(fieldz: FormField*) = for (field <- fieldz) addField(field)

	def addFeedback(feedback: Feedback) {
		//if (feedbacks.filter(_.universityId == "a").isEmpty){
		feedbacks.add(feedback)
		feedback.assignment = this
	}

	// returns feedback for a specified student
	def findFeedback(uniId: String) = feedbacks.find(_.universityId == uniId)

	// Help views decide whether to show a publish button.
	def canPublishFeedback: Boolean =
		!feedbacks.isEmpty &&
		  !unreleasedFeedback.isEmpty &&
		  closeDate.isBeforeNow

	def canSubmit(user: User): Boolean = {
		if (restrictSubmissions) {
			assignmentService.isStudentMember(user, assessmentGroup, members)
		} else {
			true
		}
	}

	/**
	 * Report on the submissions and feedbacks, noting
	 * where the lists of students don't match up.
	 */
	def submissionsReport = {
		// Get sets of University IDs
		val feedbackUniIds = feedbacks.map {
			_.universityId
		}.toSet
		val submissionUniIds = submissions.map {
			_.universityId
		}.toSet

		// Subtract the sets from each other to obtain discrepencies
		val feedbackOnly = feedbackUniIds &~ submissionUniIds
		val submissionOnly = submissionUniIds &~ feedbackUniIds

		/**
		 * We want to show a warning if some feedback items are missing either marks or attachments
		 * If however, all feedback items have only marks or attachments then we don't send a warning.
		 *
		 * We can never have a situation where no feedbacks have marks or attachments as they need to 
		 * have one or the other to exist in the first place.
		 */
		val withoutAttachments = feedbacks.filter(!_.hasAttachments).map {
			_.universityId
		}.toSet

		val withoutMarks = feedbacks.filter(feedback => !feedback.hasMark && !feedback.hasGrade).map {
			_.universityId
		}.toSet

		SubmissionsReport(this, feedbackOnly, submissionOnly, withoutAttachments, withoutMarks)
	}

	def toStringProps = Seq(
		"id" -> id,
		"name" -> name,
		"openDate" -> openDate,
		"closeDate" -> closeDate,
		"module" -> module
	)

}


case class SubmissionsReport(val assignment: Assignment, val feedbackOnly: Set[String], val submissionOnly: Set[String], val withoutAttachments: Set[String], val withoutMarks: Set[String]) {

	def hasProblems = {
		var problems = assignment.collectSubmissions && (!feedbackOnly.isEmpty || !submissionOnly.isEmpty)
		//TODO feature check
		if (assignment.collectMarks) {
			problems = problems || !withoutAttachments.isEmpty || !withoutMarks.isEmpty
		}
		problems
	}
}