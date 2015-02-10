package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import javax.persistence._
import javax.persistence.FetchType._
import javax.persistence.CascadeType._
import org.hibernate.annotations.{Filter, FilterDef, BatchSize, Type}
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

		val IncludeInFeedbackReportWithoutSubmissions = "includeInFeedbackReportWithoutSubmissions"
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
@Access(AccessType.FIELD)
class Assignment
		extends GeneratedId
		with CanBeDeleted
		with ToString
		with PermissionsTarget
		with HasSettings
		with PostLoadBehaviour
		with Serializable
		with ToEntityReference {

	import Assignment._

	type Entity = Assignment

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
	var academicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(new DateTime())

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
	var dissertation: JBoolean = _
	var allowExtensions: JBoolean = _
	var genericFeedback: String = ""

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "module_id")
	var module: Module = _

	def permissionsParents = Option(module).toStream

	@OneToMany(mappedBy = "assignment", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size = 200)
	var assessmentGroups: JList[AssessmentGroup] = JArrayList()

	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	@OrderBy("submittedDate")
	@BatchSize(size = 200)
	var submissions: JList[Submission] = JArrayList()

	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	@BatchSize(size = 200)
	var extensions: JList[Extension] = JArrayList()

	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	@BatchSize(size = 200)
	var feedbacks: JList[Feedback] = JArrayList()

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "feedback_template_id")
	@BatchSize(size = 200)
	var feedbackTemplate: FeedbackTemplate = _

	def includeInFeedbackReportWithoutSubmissions = getBooleanSetting(Settings.IncludeInFeedbackReportWithoutSubmissions, default = false)
	def includeInFeedbackReportWithoutSubmissions_= (include: Boolean) = settings += (Settings.IncludeInFeedbackReportWithoutSubmissions -> include)

	def hasFeedbackTemplate: Boolean = feedbackTemplate != null

	@transient
	lazy val workingDaysHelper = new WorkingDaysHelperImpl

	def feedbackDeadline: Option[LocalDate] = if (openEnded || dissertation) {
		None
	} else if (!hasExtensions || !extensions.exists(_.approved) || submissions.exists(s => !extensions.exists(e => e.isForUser(s.universityId, s.userId)))) {
		Option(workingDaysHelper.datePlusWorkingDays(closeDate.toLocalDate, Feedback.PublishDeadlineInWorkingDays))
	} else {
		Option(extensions.filter(_.approved).map(_.feedbackDeadline.toLocalDate).min)
	}

	def feedbackDeadlineForSubmission(submission: Submission) = feedbackDeadline.map { wholeAssignmentDeadline =>
		// If we have an extension, use the extension's expiry date
		val extension = extensions.asScala.find { e => e.isForUser(submission.universityId, submission.userId) && e.approved }

		val baseFeedbackDeadline =
			extension.map { _.feedbackDeadline.toLocalDate }.getOrElse(wholeAssignmentDeadline)

		// If the submission was late, allow 20 days from the submission day
		if (submission.isLate)
			workingDaysHelper.datePlusWorkingDays(submission.submittedDate.toLocalDate, Feedback.PublishDeadlineInWorkingDays)
		else
			baseFeedbackDeadline
	}

	def feedbackDeadlineWorkingDaysAway: Option[Int] = feedbackDeadline.map { deadline =>
		val now = LocalDate.now

		// need an offset, as the helper always includes both start and end date, off-by-one from what we want to show
		val offset =
			if (deadline.isBefore(now)) 1
			else -1 // today or in the future

		workingDaysHelper.getNumWorkingDays(now, deadline) + offset
	}

	// sort order is unpredictable on retrieval from Hibernate; use indexed defs below for access
	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL))
	@BatchSize(size = 200)
	var fields: JList[FormField] = JArrayList()

	// IndexColumn is a busted flush for fields because of reuse of non-uniqueness.
	// Use manual position management on add/removeFields, and in these getters
	def submissionFields: Seq[FormField] = fields.filter(_.context == FormFieldContext.Submission).sortBy(_.position)

	def feedbackFields: Seq[FormField] = fields.filter(_.context == FormFieldContext.Feedback).sortBy(_.position)

	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "membersgroup_id")
	private var _members: UserGroup = UserGroup.ofUsercodes

	def members: UnspecifiedTypeUserGroup = {
		Option(_members).map {
			new UserGroupCacheManager(_, assignmentMembershipService.assignmentManualMembershipHelper)
		}.orNull
	}

	def members_=(group: UserGroup) {
		_members = group
	}

	// TAB-1446 If hibernate sets members to null, make a new empty usergroup
	override def postLoad() {
		ensureMembersGroup
		ensureSettings
	}

	def ensureMembersGroup = {
		if (_members == null) _members = UserGroup.ofUsercodes
		_members
	}

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "markscheme_id")
	var markingWorkflow: MarkingWorkflow = _

	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL), orphanRemoval = true)
	@BatchSize(size = 200)
	var firstMarkers: JList[FirstMarkersMap] = JArrayList()

	/** Map between first markers and the students assigned to them */
	def firstMarkerMap: Map[String, UserGroup] = Option(firstMarkers).map { markers => markers.asScala.map {
		markerMap => markerMap.marker_id -> markerMap.students
	}.toMap }.getOrElse(Map())

	@OneToMany(mappedBy = "assignment", fetch = LAZY, cascade = Array(ALL), orphanRemoval = true)
	@BatchSize(size = 200)
	var secondMarkers: JList[SecondMarkersMap] = JArrayList()

	/** Map between second markers and the students assigned to them */
	def secondMarkerMap: Map[String, UserGroup] = Option(secondMarkers).map { markers => markers.asScala.map {
		markerMap => markerMap.marker_id -> markerMap.students
	}.toMap }.getOrElse(Map())

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

	def isOpened: Boolean = isOpened(new DateTime)

	/**
	 * Whether it's after the close date. Depending on the assignment
	 * we might still be allowing submissions, though.
	 */
	def isClosed(now: DateTime) = !openEnded && now.isAfter(closeDate)

	def isClosed: Boolean = isClosed(new DateTime)

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

	def submissionDeadline(user: User): DateTime = submissionDeadline(user.getWarwickId, user.getUserId)

	def submissionDeadline(universityId: String, usercode: String): DateTime =
		if (openEnded) null
		else extensions.find(e => e.isForUser(universityId, usercode) && e.approved).fold(closeDate)(_.expiryDate)

	/**
	 * Deadline taking into account any approved extension
	 */
	def submissionDeadline(submission: Submission) =
		if (openEnded) null
		else extensions.find(e => e.isForUser(submission.universityId, submission.userId) && e.approved).fold(closeDate)(_.expiryDate)

	def workingDaysLate(submission: Submission) =
		if (isLate(submission)) {
			val deadline = submissionDeadline(submission)

			val offset =
				if (deadline.toLocalTime.isAfter(submission.submittedDate.toLocalTime)) -1
				else 0

			val daysLate = workingDaysHelper.getNumWorkingDays(deadline.toLocalDate, submission.submittedDate.toLocalDate) + offset
			val lateDay = workingDaysHelper.datePlusWorkingDays(deadline.toLocalDate, daysLate)

			if (lateDay.isBefore(submission.submittedDate.toLocalDate)) daysLate + 1
			else daysLate
		} else 0

	def workingDaysLateIfSubmittedNow(universityId: String, usercode: String): Int = {
		val deadline = submissionDeadline(universityId, usercode)

		val offset =
			if (deadline.toLocalTime.isAfter(DateTime.now.toLocalTime)) -1
			else 0

		val daysLate = workingDaysHelper.getNumWorkingDays(deadline.toLocalDate, DateTime.now.toLocalDate) + offset
		val lateDay = workingDaysHelper.datePlusWorkingDays(deadline.toLocalDate, daysLate)

		if (lateDay.isBefore(DateTime.now.toLocalDate)) daysLate + 1
		else daysLate
	}

	/**
	 * retrospectively checks if a submission was an 'authorised late'
	 * called by submission.isAuthorisedLate to check against extensions
	 */
	def isAuthorisedLate(submission: Submission) =
		!openEnded && closeDate.isBefore(submission.submittedDate) && isWithinExtension(submission.universityId, submission.userId, submission.submittedDate)

	// returns extension for a specified student
	def findExtension(uniId: String) = extensions.find(_.universityId == uniId)

	def membershipInfo: AssignmentMembershipInfo = assignmentMembershipService.determineMembership(upstreamAssessmentGroups, Option(members))

	// converts the assessmentGroups to upstream assessment groups
	def upstreamAssessmentGroups: Seq[UpstreamAssessmentGroup] =
		assessmentGroups.asScala.flatMap {
			_.toUpstreamAssessmentGroup(academicYear)
		}

	/**
	 * Whether the assignment is not archived or deleted.
	 */
	def isAlive = active && !deleted && !archived

	/**
	 * Calculates whether we could submit to this assignment.
	 */
	def submittable(user: User) = isAlive && collectSubmissions && isOpened && (allowLateSubmissions || !isClosed || isWithinExtension(user))

	/**
	 * Calculates whether we could re-submit to this assignment (assuming that the current
	 * student has already submitted).
	 */
	def resubmittable(user: User) = submittable(user) && allowResubmission && (!isClosed || isWithinExtension(user))

	def mostRecentFeedbackUpload = feedbacks.maxBy {
		_.updatedDate
	}.updatedDate

	def addField(field: FormField) {
		if (field.context == null) throw new IllegalArgumentException("Field with name " + field.name + " has no context specified")
		if (fields.exists(_.name == field.name)) throw new IllegalArgumentException("Field with name " + field.name + " already exists")
		field.assignment = this
		field.position = fields.count(_.context == field.context)
		fields.add(field)
	}

	def removeField(field: FormField) {
		fields.remove(field)
		assignmentService.deleteFormField(field)
		// manually update all fields in the context to reflect their new positions
		fields.filter(_.context == field.context).zipWithIndex foreach {
			case (f, index) => f.position = index
		}
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

	def countFullFeedback = fullFeedback.size

	def hasFullFeedback = countFullFeedback > 0

	/**
	 * Returns a filtered copy of the feedbacks that haven't yet been published.
	 * If the old-style assignment-wide published flag is true, then it
	 * assumes all feedback has already been published.
	 */
	// scalastyle:off
	def unreleasedFeedback = fullFeedback.filterNot(_.released == true) // ==true because can be null

	// safer to use in overview pages like the department homepage as does not require the feedback list to be inflated
	def countReleasedFeedback = feedbackService.countPublishedFeedback(this)

	def countUnreleasedFeedback = countFullFeedback - countReleasedFeedback

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
		fullFeedback.nonEmpty &&
			unreleasedFeedback.nonEmpty &&
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

	def extensionsPossible: Boolean = allowExtensions && !openEnded && module.adminDepartment.allowExtensionRequests

	def newExtensionsCanBeRequested: Boolean = !isClosed && extensionsPossible

	def isMarker(user: User) = isFirstMarker(user) || isSecondMarker(user)

	def isFirstMarker(user: User): Boolean = {
		if (markingWorkflow != null)
			markingWorkflow.firstMarkers.includesUser(user)
		else false
	}

	def isSecondMarker(user: User): Boolean = {
		if (markingWorkflow != null)
			markingWorkflow.secondMarkers.includesUser(user)
		else false
	}

	def isThirdMarker(user: User): Boolean = {
		if (markingWorkflow != null)
			markingWorkflow.thirdMarkers.includesUser(user)
		else false
	}

	def isReleasedForMarking(submission: Submission): Boolean =
		feedbacks.find(_.universityId == submission.universityId) match {
			case Some(f) => f.firstMarkerFeedback != null
			case _ => false
		}

	def isReleasedToSecondMarker(submission: Submission): Boolean =
		feedbacks.find(_.universityId == submission.universityId) match {
			case Some(f) => f.secondMarkerFeedback != null
			case _ => false
		}

	def isReleasedToThirdMarker(submission: Submission): Boolean =
		feedbacks.find(_.universityId == submission.universityId) match {
			case Some(f) => f.thirdMarkerFeedback != null
			case _ => false
		}

	def getMarkerFeedback(uniId: String, user: User, feedbackPosition: FeedbackPosition): Option[MarkerFeedback] = {
		val parentFeedback = feedbacks.find(_.universityId == uniId)
		parentFeedback.flatMap {
			f => getMarkerFeedbackForPositionInFeedback(uniId, user, feedbackPosition, f)
		}
	}

	def getMarkerFeedbackForCurrentPosition(uniId: String, user: User): Option[MarkerFeedback] = for {
		feedback <- feedbacks.find(_.universityId == uniId)
		position <- feedback.getCurrentWorkflowFeedbackPosition
		markerFeedback <- getMarkerFeedbackForPositionInFeedback(uniId, user, position, feedback)
	} yield markerFeedback

	def getLatestCompletedMarkerFeedback(uniId: String, user: User): Option[MarkerFeedback] = {
		val parentFeedback = feedbacks.find(_.universityId == uniId)
		parentFeedback.flatMap {
			f => getUpToThirdFeedbacks(user, f).find(_.state == MarkingState.MarkingCompleted)
		}
	}

	def getAllMarkerFeedbacks(uniId: String, user: User): Seq[MarkerFeedback] = {
		feedbacks.find(_.universityId == uniId).fold(Seq[MarkerFeedback]())(feedback =>
			feedback.getCurrentWorkflowFeedbackPosition match {
				case None => getUpToThirdFeedbacks(user, feedback)
				case Some(ThirdFeedback) => getUpToThirdFeedbacks(user, feedback)
				case Some(SecondFeedback) => getUpToSecondFeedbacks(user, feedback)
				case Some(FirstFeedback) => getUpToFirstFeedbacks(user, feedback)
			})
	}

	private def getUpToThirdFeedbacks(user: User, feedback: Feedback): Seq[MarkerFeedback] = {
		if (this.markingWorkflow.hasThirdMarker && this.markingWorkflow.getStudentsThirdMarker(this, feedback.universityId).exists(_ == user.getUserId)) {
			Seq(feedback.retrieveThirdMarkerFeedback, feedback.retrieveSecondMarkerFeedback, feedback.retrieveFirstMarkerFeedback)
		} else {
			getUpToSecondFeedbacks(user, feedback)
		}
	}

	private def getUpToSecondFeedbacks(user: User, feedback: Feedback): Seq[MarkerFeedback] = {
		if (this.markingWorkflow.hasSecondMarker && this.markingWorkflow.getStudentsSecondMarker(this, feedback.universityId).exists(_ == user.getUserId)) {
			Seq(feedback.retrieveSecondMarkerFeedback, feedback.retrieveFirstMarkerFeedback)
		} else {
			getUpToFirstFeedbacks(user, feedback)
		}
	}

	private def getUpToFirstFeedbacks(user: User, feedback: Feedback): Seq[MarkerFeedback] = {
		if (this.markingWorkflow.getStudentsFirstMarker(this, feedback.universityId).exists(_ == user.getUserId)) {
			Seq(feedback.retrieveFirstMarkerFeedback)
		} else {
			Seq()
		}
	}

	private def getMarkerFeedbackForPositionInFeedback(uniId: String, user: User, feedbackPosition: FeedbackPosition, feedback: Feedback): Option[MarkerFeedback] = {
		feedbackPosition match {
			case FirstFeedback =>
				if (this.isFirstMarker(user))
					Some(feedback.retrieveFirstMarkerFeedback)
				else
					None
			case SecondFeedback =>
				if (this.markingWorkflow.hasSecondMarker && this.isSecondMarker(user))
					Some(feedback.retrieveSecondMarkerFeedback)
				else
					None
			case ThirdFeedback =>
				if (this.markingWorkflow.hasThirdMarker && this.isThirdMarker(user))
					Some(feedback.retrieveThirdMarkerFeedback)
				else
					None
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

	// later we may do more complex checks to see if this particular markingWorkflow requires that feedback is released manually
	// for now all markingWorkflow will require you to release feedback so if one exists for this assignment - provide it
	def mustReleaseForMarking = hasWorkflow

	def hasWorkflow = markingWorkflow != null

	def needsFeedbackPublishing = {
		if (openEnded || !collectSubmissions || archived) {
			false
		} else {
			submissions.asScala.exists(s => !fullFeedback.exists(f => f.universityId == s.universityId && f.checkedReleased))
		}
	}

	def toEntityReference = new AssignmentEntityReference().put(this)

}

case class SubmissionsReport(assignment: Assignment) {

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
	val withoutAttachments = feedbacks.filter(f => !f.hasAttachments && !f.comments.exists(_.hasText)).map(toUniId).toSet
	val withoutMarks = feedbacks.filter(!_.hasMarkOrGrade).map(toUniId).toSet
	val plagiarised = submissions.filter(_.suspectPlagiarised).map(toUniId).toSet

	def hasProblems: Boolean = {
		val shouldBeEmpty = Set(feedbackOnly, submissionOnly, plagiarised)
		val problems = assignment.collectSubmissions && shouldBeEmpty.exists { _.nonEmpty }

		if (assignment.collectMarks) {
			val shouldBeEmptyWhenCollectingMarks = Set(withoutAttachments, withoutMarks)
			problems || shouldBeEmptyWhenCollectingMarks.exists { _.nonEmpty }
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
	var summative: JBoolean = true
	var dissertation: JBoolean = false
	var includeInFeedbackReportWithoutSubmissions: JBoolean = false

	def copyBooleansTo(assignment: Assignment) {
		assignment.openEnded = openEnded
		assignment.collectMarks = collectMarks
		assignment.collectSubmissions = collectSubmissions
		assignment.restrictSubmissions = restrictSubmissions
		assignment.allowLateSubmissions = allowLateSubmissions
		assignment.allowResubmission = allowResubmission
		assignment.displayPlagiarismNotice = displayPlagiarismNotice
		assignment.allowExtensions = allowExtensions
		assignment.summative = summative
		assignment.dissertation = dissertation
		assignment.includeInFeedbackReportWithoutSubmissions = includeInFeedbackReportWithoutSubmissions
	}
}


object BooleanAssignmentProperties extends BooleanAssignmentProperties {
	def apply(assignment: Assignment) = copyBooleansTo(assignment)
}
