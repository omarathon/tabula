package uk.ac.warwick.tabula.data.model.groups

import org.hibernate.annotations.{AccessType, Filter, FilterDef, IndexColumn, Type}
import javax.persistence._
import javax.persistence.FetchType._
import javax.persistence.CascadeType._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import javax.persistence._
import javax.persistence.FetchType._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.data.model.permissions.SmallGroupGrantedRole
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types
import javax.validation.constraints.NotNull
import scala.collection.JavaConverters._

object SmallGroupSet {
	final val NotDeletedFilter = "notDeleted"
}

/**
 * Represents a set of small groups, within an instance of a module.
 */
@FilterDef(name = SmallGroupSet.NotDeletedFilter, defaultCondition = "deleted = 0")
@Filter(name = SmallGroupSet.NotDeletedFilter)
@Entity
@AccessType("field")
class SmallGroupSet extends GeneratedId with CanBeDeleted with ToString with PermissionsTarget {
	import SmallGroupSet._
	
	@transient var permissionsService = Wire[PermissionsService]
	@transient var membershipService = Wire[AssignmentMembershipService]

	def this(_module: Module) {
		this()
		this.module = _module
	}

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime())

	@NotNull
	var name: String = _

	var archived: JBoolean = false

	var released: JBoolean = false
	
	@Column(name="group_format")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.groups.SmallGroupFormatUserType")
	@NotNull
	var format: SmallGroupFormat = _

	@ManyToOne
	@JoinColumn(name = "module_id")
	var module: Module = _
	
	@OneToMany(mappedBy = "groupSet", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	var groups: JList[SmallGroup] = JArrayList()

	@OneToOne(cascade = Array(ALL))
	@JoinColumn(name = "membersgroup_id")
	var members: UserGroup = new UserGroup
	
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name="smallgroupset_assessmentgroup",
		joinColumns=Array(new JoinColumn(name="smallgroupset_id")),
		inverseJoinColumns=Array(new JoinColumn(name="assessmentgroup_id")))
	var assessmentGroups: JList[UpstreamAssessmentGroup] = JArrayList()
	
	def unallocatedStudents = {
		val allStudents = membershipService.determineMembershipUsers(assessmentGroups.asScala, Some(members))
		val allocatedStudents = groups.asScala flatMap { _.students.users }
		
		allStudents diff allocatedStudents
	}
	
	def hasAllocated = groups.asScala exists { !_.students.isEmpty }
	
	def permissionsParents = Option(module).toStream

	def toStringProps = Seq(
		"id" -> id,
		"name" -> name,
		"module" -> module)

}

sealed abstract class SmallGroupFormat(val code: String, val description: String) {
	// For Spring, the silly bum
	def getCode = code
	def getDescription = description
	
	override def toString = description
}

object SmallGroupFormat {
	case object Seminar extends SmallGroupFormat("seminar", "Seminar")
	case object Lab extends SmallGroupFormat("lab", "Lab")
	case object Tutorial extends SmallGroupFormat("tutorial", "Tutorial")
	case object Project extends SmallGroupFormat("project", "Project group")
	case object Example extends SmallGroupFormat("example", "Example Class")

	// lame manual collection. Keep in sync with the case objects above
	val members = Set(Seminar, Lab, Tutorial, Project, Example)

	def fromCode(code: String) =
		if (code == null) null
		else members.find{_.code == code} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}

	def fromDescription(description: String) =
		if (description == null) null
		else members.find{_.description == description} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}
}

class SmallGroupFormatUserType extends AbstractBasicUserType[SmallGroupFormat, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = SmallGroupFormat.fromCode(string)
	override def convertToValue(format: SmallGroupFormat) = format.code
}