package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType._
import javax.persistence.FetchType._
import javax.persistence.{CascadeType, Entity, _}

import org.hibernate.annotations.{Filter, FilterDef, AccessType, BatchSize, Type}
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, UserGroupCacheManager}

import scala.collection.JavaConverters._

object Exam {
	final val NotDeletedFilter = "notDeleted"
	final val defaultFeedbackTextFieldName = "feedbackText"
}

@FilterDef(name = Exam.NotDeletedFilter, defaultCondition = "deleted = 0")
@Filter(name = Exam.NotDeletedFilter)
@Entity
@AccessType("field")
class Exam
	extends GeneratedId
	with CanBeDeleted
	with PermissionsTarget
	with ToEntityReference
	with PostLoadBehaviour
	with Serializable {

	type Entity = Exam

	@transient
	var examMembershipService = Wire[AssessmentMembershipService]("assignmentMembershipService")

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "module_id")
	var module: Module = _

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(new DateTime())

	var name: String = _

	@OneToMany(mappedBy = "exam", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size = 200)
	var assessmentGroups: JList[AssessmentGroup] = JArrayList()

	@OneToMany(mappedBy = "exam", fetch = LAZY, cascade = Array(ALL))
	@BatchSize(size = 200)
	var feedbacks: JList[ExamFeedback] = JArrayList()

	// sort order is unpredictable on retrieval from Hibernate; use indexed defs below for access
	@OneToMany(mappedBy = "exam", fetch = LAZY, cascade = Array(ALL))
	@BatchSize(size = 200)
	var fields: JList[ExamFormField] = JArrayList()

	def feedbackFields: Seq[ExamFormField] = fields.asScala.filter(_.context == FormFieldContext.Feedback).sortBy(_.position)

	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "membersgroup_id")
	private var _members: UserGroup = UserGroup.ofUsercodes

	def members: UnspecifiedTypeUserGroup = {
		Option(_members).map {
			new UserGroupCacheManager(_, examMembershipService.assignmentManualMembershipHelper)
		}.orNull
	}

	def members_=(group: UserGroup) {
		_members = group
	}

	// TAB-1446 If hibernate sets members to null, make a new empty usergroup
	override def postLoad() {
		ensureMembersGroup
	}

	def ensureMembersGroup = {
		if (_members == null) _members = UserGroup.ofUsercodes
		_members
	}

	def addField(field: ExamFormField) {
		if (field.context == null) throw new IllegalArgumentException("Field with name " + field.name + " has no context specified")
		if (fields.asScala.exists(_.name == field.name)) throw new IllegalArgumentException("Field with name " + field.name + " already exists")
		field.exam = this
		field.position = fields.asScala.count(_.context == field.context)
		fields.add(field)
	}

	def addDefaultFeedbackFields() {
		val feedback = new ExamTextField
		feedback.name = Exam.defaultFeedbackTextFieldName
		feedback.value = ""
		feedback.context = FormFieldContext.Feedback

		addField(feedback)
	}

	def addDefaultFields() {
		addDefaultFeedbackFields()
	}

	def permissionsParents = Option(module).toStream

	def toEntityReference = new ExamEntityReference().put(this)

}
