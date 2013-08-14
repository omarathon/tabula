package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.reflect.Manifest
import org.hibernate.annotations.{AccessType, Filter, FilterDef, IndexColumn, Type}
import javax.persistence._
import javax.persistence.FetchType._
import javax.persistence.CascadeType._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.forms.WordCountField
import uk.ac.warwick.tabula.data.model.MarkingMethod._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.data.model.permissions.AssignmentGrantedRole
import org.hibernate.annotations.ForeignKey
import javax.persistence._
import javax.persistence.FetchType._
import javax.persistence.CascadeType._
import scala.reflect._
import scala.collection.mutable

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
class Assignment extends GeneratedId with CanBeDeleted with ToString with PermissionsTarget {
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

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var openDate: DateTime = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var closeDate: DateTime = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var createdDate = DateTime.now()

	var openEnded: JBoolean = false
	var collectMarks: JBoolean = false
	var collectSubmissions: JBoolean = false
	var restrictSubmissions: JBoolean = false
	var allowLateSubmissions: JBoolean = true
	var allowResubmission: JBoolean = false
	var displayPlagiarismNotice: JBoolean = false
	var summative: JBoolean = true

	var allowExtensions: JBoolean = false
	// allow students to request extensions via the app

	var allowExtensionRequests: JBoolean = false

	@ManyToOne
	@JoinColumn(name = "module_id")
	var module: Module = _

	def permissionsParents = Option(module).toStream

	@OneToMany(mappedBy = "assignment", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	var assessmentGroups: JList[AssessmentGroup] = JArrayList()

	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	@OrderBy("submittedDate")
	var submissions: JList[Submission] = JArrayList()

	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	var extensions: JList[Extension] = JArrayList()

	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	var feedbacks: JList[Feedback] = JArrayList()

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "feedback_template_id")
	var feedbackTemplate: FeedbackTemplate = _

	def hasFeedbackTemplate: Boolean = feedbackTemplate != null

	/**
	 * FIXME IndexColumn doesn't work, currently setting position manually. Investigate!
	 */
	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	@IndexColumn(name = "position")
	var fields: JList[FormField] = JArrayList()

	def submissionFields: Seq[FormField] = fields.filter(_.context == FormFieldContext.Submission)
	def feedbackFields: Seq[FormField] = fields.filter(_.context == FormFieldContext.Feedback)

	@OneToOne(cascade = Array(ALL))
	@JoinColumn(name = "membersgroup_id")
	var members: UserGroup = UserGroup.ofUsercodes

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
		!openEnded && closeDate.isBefore(submission.submittedDate) && !isWithinExtension(submission.userId, submission.submittedDate)

	/**
	 * retrospectively checks if a submission was an 'authorised late'
	 * called by submission.isAuthorisedLate to check against extensions
	 */
	def isAuthorisedLate(submission: Submission) =
		!openEnded && closeDate.isBefore(submission.submittedDate) && isWithinExtension(submission.userId, submission.submittedDate)

	// returns extension for a specified student
	def findExtension(uniId: String) = extensions.find(_.universityId == uniId)

	def membershipInfo = assignmentMembershipService.determineMembership(upstreamAssessmentGroups, Option(members))

	// converts the assessmentGroups to upstream assessment groups
	def upstreamAssessmentGroups: Seq[UpstreamAssessmentGroup] = {
		if(academicYear == null){
			Seq()
		}
		else {
			val validGroups = assessmentGroups.filterNot(group=> group.upstreamAssignment == null || group.occurrence == null)
			validGroups.flatMap{group =>
				val template = new UpstreamAssessmentGroup
				template.academicYear = academicYear
				template.assessmentGroup = group.upstreamAssignment.assessmentGroup
				template.moduleCode = group.upstreamAssignment.moduleCode
				template.occurrence = group.occurrence
				assignmentMembershipService.getUpstreamAssessmentGroup(template)
			}
		}
	}

	/**
	 * Whether the assignment is not archived or deleted.
	 */
	def isAlive = active && !deleted && !archived

	/**
	 * Calculates whether we could submit to this assignment.
	 */
	def submittable(uniId: String) = isAlive && collectSubmissions && isOpened() && (allowLateSubmissions || !isClosed() || isWithinExtension(uniId))

	/**
	 * Calculates whether we could re-submit to this assignment (assuming that the current
	 * student has already submitted).
	 */
	def resubmittable(uniId: String) = submittable(uniId) && allowResubmission && (!isClosed() || isWithinExtension(uniId))

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
	def countUnapprovedExtensions = extensionService.countUnapprovedExtensions(this)


	def addFields(fieldz: FormField*) = for (field <- fieldz) addField(field)

	def addFeedback(feedback: Feedback) {
		feedbacks.add(feedback)
		feedback.assignment = this
	}

	def addSubmission(submission: Submission) {
		submissions.add(submission)
		submission.assignment = this
	}

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
			// users can always submit to assignments if they have a submission or peice of feedback
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
	def getStudentsFirstMarker(submission: Submission): Option[String] = Option(markingWorkflow) flatMap {_.markingMethod match {
		case SeenSecondMarking =>  {
			val mapEntry = Option(markerMap) flatMap {_.find{p:(String,UserGroup) =>
				p._2.includes(submission.userId) && markingWorkflow.firstMarkers.includes(p._1)
			}}
			mapEntry match {
				case Some((markerId, students)) => Some(markerId)
				case _ => None
			}
		}
		case StudentsChooseMarker => markerSelectField match {
			case Some(field) => {
				submission.getValue(field) match {
					case Some(sv) => Some(sv.value)
					case None => None
				}
			}
			case None => None
		}
		case _ => None
	}}

	def getStudentsSecondMarker(submission: Submission): Option[String] = Option(markingWorkflow) flatMap {_.markingMethod match {
		case SeenSecondMarking =>  {
			val mapEntry = markerMap.find{p:(String,UserGroup) =>
				p._2.includes(submission.userId) && markingWorkflow.secondMarkers.includes(p._1)
			}
			mapEntry match {
				case Some((markerId, students)) => Some(markerId)
				case _ => None
			}
		}
		case _ => None
	}}

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
	var grantedRoles:JList[AssignmentGrantedRole] = JArrayList()

	def toStringProps = Seq(
		"id" -> id,
		"name" -> name,
		"openDate" -> openDate,
		"closeDate" -> closeDate,
		"module" -> module)

    def getUniIdsWithSubmissionOrFeedback = {
        var idsWithSubmissionOrFeedback: Set[String] = Set()

        for (submission <- submissions) idsWithSubmissionOrFeedback += submission.universityId
        for (feedback <- fullFeedback) idsWithSubmissionOrFeedback += feedback.universityId

        idsWithSubmissionOrFeedback
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
	 *
	 * We can never have a situation where no feedbacks have marks or attachments as they need to
	 * have one or the other to exist in the first place.
	 */
	val withoutAttachments = feedbacks.filter(!_.hasAttachments).map(toUniId).toSet
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
