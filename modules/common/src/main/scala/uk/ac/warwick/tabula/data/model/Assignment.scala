package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import javax.persistence._
import javax.persistence.FetchType._
import javax.persistence.CascadeType._
import org.hibernate.annotations.{ForeignKey, Filter, FilterDef, AccessType, BatchSize, Type, IndexColumn}
import org.joda.time.{LocalDate, DateTime}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.forms.WordCountField
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.data.model.permissions.AssignmentGrantedRole
import scala.reflect._
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.helpers.StringUtils._


object Assignment {
	// don't use the same name in different contexts, as that will kill find methods
	val defaultCommentFieldName = "pretext"
	val defaultUploadName = "upload"
	val defaultFeedbackTextFieldName = "feedbackText"
	val defaultMarkerSelectorName = "marker"
	val defaultWordCountName = "wordcount"
	final val NotDeletedFilter = "notDeleted"
	final val MaximumFileAttachments = 50
	final val MaximumWordCount = 1000000

	object Settings {
		object InfoViewType {
			val Default = "default"
			val Table = "table"
			val Summary = "summary"
		}
	}
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
class Assignment extends GeneratedId with CanBeDeleted with ToString with PermissionsTarget with PostLoadBehaviour with Serializable {
	import Assignment._

	@transient
	var assignmentService = Wire[AssignmentService]("assignmentService")

	@transient
	var assignmentMembershipService = Wire[AssignmentMembershipService]("assignmentMembershipService")

	@transient
	var feedbackService = Wire[FeedbackService]("feedbackService")

	@transient
	var extensionService = Wire[ExtensionService]("extensionService")

	@transient
	var userLookup = Wire[UserLookupService]("userLookup")

	def this(_module: Module) {
		this()
		this.module = _module
	}

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime())

	@Type(`type` = "uk.ac.warwick.tabula.data.model.StringListUserType")
	var fileExtensions: Seq[String] = _

	var attachmentLimit: Int = 1

	var name: String = _
	var active: JBoolean = true
	var archived: JBoolean = false

	var openDate: DateTime = _
	var closeDate: DateTime = _
	var createdDate = DateTime.now()

	// left unset as defaults are set by BooleanAssignmentProperties
	var openEnded: JBoolean = _
	var collectMarks: JBoolean = _
	var collectSubmissions: JBoolean = _
	var restrictSubmissions: JBoolean = _
	var allowLateSubmissions: JBoolean = _
	var allowResubmission: JBoolean = _
	var displayPlagiarismNotice: JBoolean = _
	var summative: JBoolean = _
	var allowExtensions: JBoolean = _
	var allowExtensionRequests: JBoolean = _ // by students
	var genericFeedback: String = ""

	@ManyToOne
	@JoinColumn(name = "module_id")
	var module: Module = _

	def permissionsParents = Option(module).toStream

	@OneToMany(mappedBy = "assignment", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var assessmentGroups: JList[AssessmentGroup] = JArrayList()

	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	@OrderBy("submittedDate")
	@BatchSize(size=200)
	var submissions: JList[Submission] = JArrayList()

	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	@BatchSize(size=200)
	var extensions: JList[Extension] = JArrayList()

	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	@BatchSize(size=200)
	var feedbacks: JList[Feedback] = JArrayList()

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "feedback_template_id")
	@BatchSize(size=200)
	var feedbackTemplate: FeedbackTemplate = _

	def hasFeedbackTemplate: Boolean = feedbackTemplate != null

	@transient
	lazy val workingDaysHelper = new WorkingDaysHelperImpl

	def feedbackDeadline: Option[LocalDate] = if (openEnded) {
		None
	} else {
		Option(workingDaysHelper.datePlusWorkingDays(closeDate.toLocalDate, Feedback.PublishDeadlineInWorkingDays))
	}

	def feedbackDeadlineWorkingDaysAway: Option[Int] = if (openEnded) {
		None
	} else {
		val now = LocalDate.now
		val deadline = workingDaysHelper.datePlusWorkingDays(closeDate.toLocalDate, Feedback.PublishDeadlineInWorkingDays)

		// need an offset, as the helper always includes both start and end date, off-by-one from what we want to show
		val offset =
			if (deadline.isBefore(now)) 1
			else -1 // today or in the future

		Option(workingDaysHelper.getNumWorkingDays(now, deadline) + offset)
	}

	// sort order is unpredictable on retrieval from Hibernate; use indexed defs below for access
	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	@BatchSize(size=200)
	var fields: JList[FormField] = JArrayList()

	// IndexColumn is a busted flush for fields because of reuse of non-uniqueness.
	// Use manual position management on add/removeFields, and in these getters
	def submissionFields: Seq[FormField] = fields.filter(_.context == FormFieldContext.Submission).sortBy(_.position)
	def feedbackFields: Seq[FormField] = fields.filter(_.context == FormFieldContext.Feedback).sortBy(_.position)

	@OneToOne(cascade = Array(ALL))
	@JoinColumn(name = "membersgroup_id")
	var members: UserGroup = UserGroup.ofUsercodes

	// TAB-1446 If hibernate sets members to null, make a new empty usergroup
	override def postLoad {
		ensureMembersGroup
	}

	def ensureMembersGroup = {
		if (members == null) members = UserGroup.ofUsercodes
		members
	}

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name="markscheme_id")
	var markingWorkflow: MarkingWorkflow = _

	/** Map between markers and the students assigned to them */
	@OneToMany @JoinTable(name="marker_usergroup")
	@MapKeyColumn(name="marker_uni_id")
	var markerMap: JMap[String, UserGroup] = JMap[String, UserGroup]()

	def setAllFileTypesAllowed() {
		fileExtensions = Nil
	}

	/**
	 * Before we allow customising of assignment submission forms, we just want the basic
	 * fields to allow you to attach a file and display some instructions.
	 */
	def addDefaultSubmissionFields() {
		val pretext = new CommentField
		pretext.name = defaultCommentFieldName
		pretext.value = ""
		pretext.context = FormFieldContext.Submission

		val file = new FileField
		file.name = defaultUploadName
		file.context = FormFieldContext.Submission

		addFields(pretext, file)
	}

	/**
	 * Before we allow customising of assignment feedback forms, we just want the basic
	 * fields to allow you to enter a comment.
	 */
	def addDefaultFeedbackFields() {
		val feedback = new TextField
		feedback.name = defaultFeedbackTextFieldName
		feedback.value = ""
		feedback.context = FormFieldContext.Feedback

		addField(feedback)
	}

	def addDefaultFields() {
		addDefaultSubmissionFields()
		addDefaultFeedbackFields()
	}

	def setDefaultBooleanProperties() {
		BooleanAssignmentProperties(this)
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
	def isClosed(now: DateTime) = !openEnded && now.isAfter(closeDate)

	def isClosed(): Boolean = isClosed(new DateTime)

	/**
	 * True if the specified user has been granted an extension and that extension has not expired on the specified date
	 */
	def isWithinExtension(user: User, time: DateTime): Boolean = isWithinExtension(user.getWarwickId, user.getUserId, time) 
	def isWithinExtension(universityId: String, usercode: String, time: DateTime) =
		extensions.exists(e => e.isForUser(universityId, usercode) && e.approved && e.expiryDate.isAfter(time))

	/**
	 * True if the specified user has been granted an extension and that extension has not expired now
	 */
	def isWithinExtension(user: User): Boolean = isWithinExtension(user, new DateTime)
	def isWithinExtension(universityId: String, usercode: String): Boolean = isWithinExtension(universityId, usercode, new DateTime)

	/**
	 * retrospectively checks if a submission was late. called by submission.isLate to check against extensions
	 */
	def isLate(submission: Submission) =
		!openEnded && closeDate.isBefore(submission.submittedDate) && !isWithinExtension(submission.universityId, submission.userId, submission.submittedDate)

	/**
	 * retrospectively checks if a submission was an 'authorised late'
	 * called by submission.isAuthorisedLate to check against extensions
	 */
	def isAuthorisedLate(submission: Submission) =
		!openEnded && closeDate.isBefore(submission.submittedDate) && isWithinExtension(submission.universityId, submission.userId, submission.submittedDate)

	// returns extension for a specified student
	def findExtension(uniId: String) = extensions.find(_.universityId == uniId)

	def membershipInfo = assignmentMembershipService.determineMembership(upstreamAssessmentGroups, Option(members))

	// converts the assessmentGroups to upstream assessment groups
	def upstreamAssessmentGroups: Seq[UpstreamAssessmentGroup] = 
		assessmentGroups.asScala.flatMap { _.toUpstreamAssessmentGroup(academicYear) }

	/**
	 * Whether the assignment is not archived or deleted.
	 */
	def isAlive = active && !deleted && !archived

	/**
	 * Calculates whether we could submit to this assignment.
	 */
	def submittable(user: User) = isAlive && collectSubmissions && isOpened() && (allowLateSubmissions || !isClosed() || isWithinExtension(user))

	/**
	 * Calculates whether we could re-submit to this assignment (assuming that the current
	 * student has already submitted).
	 */
	def resubmittable(user: User) = submittable(user) && allowResubmission && (!isClosed() || isWithinExtension(user))

	def mostRecentFeedbackUpload = feedbacks.maxBy {
		_.uploadedDate
	}.uploadedDate

	def addField(field: FormField) {
		if (field.context == null) throw new IllegalArgumentException("Field with name " + field.name + " has no context specified")
		if (fields.exists(_.name == field.name)) throw new IllegalArgumentException("Field with name " + field.name + " already exists")
		field.assignment = this
		field.position = fields.filter(_.context == field.context).length
		fields.add(field)
	}

	def removeField(field: FormField) {
		fields.remove(field)
		assignmentService.deleteFormField(field)
		// manually update all fields in the context to reflect their new positions
		fields.filter(_.context == field.context).zipWithIndex foreach {case (field, index) => field.position = index}
	}

	def attachmentField: Option[FileField] = findFieldOfType[FileField](Assignment.defaultUploadName)

	def commentField: Option[CommentField] = findFieldOfType[CommentField](Assignment.defaultCommentFieldName)

	def markerSelectField: Option[MarkerSelectField] =
		findFieldOfType[MarkerSelectField](Assignment.defaultMarkerSelectorName)

	def wordCountField: Option[WordCountField] = findFieldOfType[WordCountField](Assignment.defaultWordCountName)

	def feedbackCommentsField: Option[TextField] = findFieldOfType[TextField](Assignment.defaultFeedbackTextFieldName)

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
	def findFieldOfType[A <: FormField : ClassTag](name: String): Option[A] =
		findField(name) match {
			case Some(field) if classTag[A].runtimeClass.isInstance(field) => Some(field.asInstanceOf[A])
			case _ => None
		}

	// feedback that has been been through the marking process (not placeholders for marker feedback)
	def fullFeedback = feedbacks.filterNot(_.isPlaceholder).toSeq
	// safer to use in overview pages like the department homepage as does not require the feedback list to be inflated
	def countFullFeedback = feedbackService.countFullFeedback(this)
	def hasFullFeedback = countFullFeedback > 0

	/**
	 * Returns a filtered copy of the feedbacks that haven't yet been published.
	 * If the old-style assignment-wide published flag is true, then it
	 * assumes all feedback has already been published.
	 */
	// scalastyle:off
	def unreleasedFeedback = fullFeedback.filterNot(_.released == true) // ==true because can be null

	// safer to use in overview pages like the department homepage as does not require the feedback list to be inflated
	def countReleasedFeedback  = feedbackService.countPublishedFeedback(this)
	def countUnreleasedFeedback  = countFullFeedback - countReleasedFeedback
	def hasReleasedFeedback = countReleasedFeedback > 0
	def hasUnreleasedFeedback = countReleasedFeedback < countFullFeedback
	def countExtensions = extensionService.countExtensions(this)
	def hasExtensions = extensionService.hasExtensions(this)
	def countUnapprovedExtensions = extensionService.countUnapprovedExtensions(this)
	def hasUnapprovedExtensions = extensionService.hasUnapprovedExtensions(this)
	def getUnapprovedExtensions = extensionService.getUnapprovedExtensions(this)

	def addFields(fieldz: FormField*) = for (field <- fieldz) addField(field)

	def addFeedback(feedback: Feedback) {
		feedbacks.add(feedback)
		feedback.assignment = this
	}

	def addSubmission(submission: Submission) {
		submissions.add(submission)
		submission.assignment = this
	}

	// returns the submission for a specified student
	def findSubmission(uniId: String) = submissions.find(_.universityId == uniId)

	// returns feedback for a specified student
	def findFeedback(uniId: String) = feedbacks.find(_.universityId == uniId)

	// returns feedback for a specified student
	def findFullFeedback(uniId: String) = fullFeedback.find(_.universityId == uniId)

	// Help views decide whether to show a publish button.
	def canPublishFeedback: Boolean =
		!fullFeedback.isEmpty &&
			!unreleasedFeedback.isEmpty &&
			(closeDate.isBeforeNow || openEnded)

	def canSubmit(user: User): Boolean = {
		if (restrictSubmissions) {
			// users can always submit to assignments if they have a submission or piece of feedback
			submissions.asScala.exists(_.universityId == user.getWarwickId) ||
			fullFeedback.exists(_.universityId == user.getWarwickId) ||
			assignmentMembershipService.isStudentMember(user, upstreamAssessmentGroups, Option(members))
		} else {
			true
		}
	}

	def isMarker(user: User) = isFirstMarker(user)|| isSecondMarker(user)

	def isFirstMarker(user: User): Boolean = {
		if (markingWorkflow != null)
			markingWorkflow.firstMarkers.includes(user.getUserId)
		else false
	}

	def isSecondMarker(user: User): Boolean = {
		if (markingWorkflow != null)
			markingWorkflow.secondMarkers.includes(user.getUserId)
		else false
	}

	def isReleasedForMarking(submission:Submission) : Boolean =
		feedbacks.find(_.universityId == submission.universityId) match {
			case Some(f) => f.firstMarkerFeedback != null
			case _ => false
		}

	def isReleasedToSecondMarker(submission:Submission) : Boolean =
		feedbacks.find(_.universityId == submission.universityId) match {
			case Some(f) => f.secondMarkerFeedback != null
			case _ => false
		}

	/*
		get a MarkerFeedback for the given student ID and user  if one exists. firstMarker = true returns the first markers feedback item.
		false returns the second markers item
	 */
	def getMarkerFeedback(uniId:String, user:User) : Option[MarkerFeedback] = {
		val parentFeedback = feedbacks.find(_.universityId == uniId)
		parentFeedback match {
			case Some(f) => {
				if(this.isFirstMarker(user))
					Some(f.retrieveFirstMarkerFeedback)
				else if(this.isSecondMarker(user))
					Some(f.retrieveSecondMarkerFeedback)
				else
					None
			}
			case None => None
		}
	}

	/**
	 * Optionally returns the first marker for the given submission
	 * Returns none if this assignment doesn't have a valid marking workflow attached
	 */
	def getStudentsFirstMarker(submission: Submission): Option[String] =
		Option(markingWorkflow) flatMap {_.getStudentsFirstMarker(this, submission.universityId)}

	def getStudentsSecondMarker(submission: Submission): Option[String] =
		Option(markingWorkflow) flatMap {_.getStudentsSecondMarker(this, submission.universityId)}

		/**
	 * Optionally returns the submissions that are to be marked by the given user
	 * Returns none if this assignment doesn't have a valid marking workflow attached
	 */
	def getMarkersSubmissions(marker: User): Seq[Submission] = {
		if (markingWorkflow != null) markingWorkflow.getSubmissions(this, marker)
		else Seq()
	}

	/**
	 * Report on the submissions and feedbacks, noting
	 * where the lists of students don't match up.
	 */
	def submissionsReport = SubmissionsReport(this)

	@OneToMany(mappedBy="scope", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	@ForeignKey(name="none")
	@BatchSize(size=200)
	var grantedRoles:JList[AssignmentGrantedRole] = JArrayList()

	def toStringProps = Seq(
		"id" -> id,
		"name" -> name,
		"openDate" -> openDate,
		"closeDate" -> closeDate,
		"module" -> module)

    def getUniIdsWithSubmissionOrFeedback = {
				val submissionIds = submissions.asScala.map { _.universityId }.toSet
				val feedbackIds = fullFeedback.map { _.universityId }.toSet
				
				submissionIds ++ feedbackIds
    }

}

case class SubmissionsReport(val assignment: Assignment) {

	private def feedbacks = assignment.fullFeedback
	private def submissions = assignment.submissions

	// Get sets of University IDs
	private val feedbackUniIds = feedbacks.map(toUniId).toSet
	private val submissionUniIds = submissions.map(toUniId).toSet

	// Subtract the sets from each other to obtain discrepancies
	val feedbackOnly = feedbackUniIds &~ submissionUniIds
	val submissionOnly = submissionUniIds &~ feedbackUniIds

	/**
	 * We want to show a warning if some feedback items are missing either marks or attachments
	 * If however, all feedback items have only marks or attachments then we don't send a warning.
	 */
	val withoutAttachments = feedbacks.filter(f => !f.hasAttachments && f.defaultFeedbackComments.filter { _.hasText }.isEmpty).map(toUniId).toSet
	val withoutMarks = feedbacks.filter(!_.hasMarkOrGrade).map(toUniId).toSet
	val plagiarised = submissions.filter(_.suspectPlagiarised).map(toUniId).toSet

	def hasProblems: Boolean = {
		val shouldBeEmpty = Set(feedbackOnly, submissionOnly, plagiarised)
		val problems = assignment.collectSubmissions && shouldBeEmpty.exists { !_.isEmpty }

		if (assignment.collectMarks) {
			val shouldBeEmptyWhenCollectingMarks = Set(withoutAttachments, withoutMarks)
			problems || shouldBeEmptyWhenCollectingMarks.exists { !_.isEmpty }
		} else {
		    problems
		}
	}

	// To make map() calls neater
    private def toUniId(f: Feedback) = f.universityId
    private def toUniId(s: Submission) = s.universityId


}

/**
 * One stop shop for setting default boolean values for assignment properties
 */
trait BooleanAssignmentProperties {
	var openEnded: JBoolean = false
	var collectMarks: JBoolean = true
	var collectSubmissions: JBoolean = true
	var restrictSubmissions: JBoolean = false
	var allowLateSubmissions: JBoolean = true
	var allowResubmission: JBoolean = true
	var displayPlagiarismNotice: JBoolean = true
	var allowExtensions: JBoolean = true
	var allowExtensionRequests: JBoolean = false
	var summative: JBoolean = true

	def copyBooleansTo(assignment: Assignment) {
		assignment.openEnded = openEnded
		assignment.collectMarks = collectMarks
		assignment.collectSubmissions = collectSubmissions
		assignment.restrictSubmissions = restrictSubmissions
		assignment.allowLateSubmissions = allowLateSubmissions
		assignment.allowResubmission = allowResubmission
		assignment.displayPlagiarismNotice = displayPlagiarismNotice
		assignment.allowExtensions = allowExtensions
		assignment.allowExtensionRequests = allowExtensionRequests
		assignment.summative = summative
	}
}


object BooleanAssignmentProperties extends BooleanAssignmentProperties {
	def apply(assignment: Assignment) = copyBooleansTo(assignment)
}
