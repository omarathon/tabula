package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConverters._
import javax.persistence._
import javax.persistence.CascadeType._
import org.joda.time.DateTime
import org.joda.time.LocalDate
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.{TermService, ProfileService, RelationshipService}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.permissions.MemberGrantedRole
import uk.ac.warwick.tabula.system.permissions.Restricted
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.AcademicYear
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.EqualsBuilder
import javax.persistence.CascadeType
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.FilterDefs
import org.hibernate.annotations.Filters
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.ForeignKey
import org.hibernate.annotations.Formula
import org.hibernate.annotations.Type
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.Filter
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking

object Member {
	final val StudentsOnlyFilter = "studentsOnly"
	final val ActiveOnlyFilter = "activeOnly"
}

/**
 * Represents an assignment within a module, occurring at a certain time.
 *
 * Notes about the notDeleted filter:
 * filters don't run on session.get() but getById will check for you.
 * queries will only include it if it's the entity after "from" and not
 * some other secondary entity joined on. It's usually possible to flip the
 * query around to make this work.
 *
 * There is no filter returning only fresh records (i.e. those seen in the last SITS import), since that would
 * prevent member.getByUniversityId from returning stale records - which would be an issue since the import
 * uses that to check if a record is there already before re-importing it.  (So having the filter in place would
 * prevent students who had been deleted from SITS from being re-imported.)
 */
@FilterDefs(Array(
		new FilterDef(name = Member.StudentsOnlyFilter, defaultCondition = "usertype = 'S'"),
		new FilterDef(name = Member.ActiveOnlyFilter, defaultCondition = "(inuseflag = 'Active' or inuseflag like 'Inactive - Starts %')")
	))
	@Filters(Array(
		new Filter(name = Member.StudentsOnlyFilter),
		new Filter(name = Member.ActiveOnlyFilter)
	))
@Entity
@AccessType("field")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
		name="userType",
		discriminatorType=DiscriminatorType.STRING
)
abstract class Member extends MemberProperties with ToString with HibernateVersioned with PermissionsTarget with Logging {

	@transient
	var profileService = Wire.auto[ProfileService]

	@transient
	var relationshipService = Wire.auto[RelationshipService]

	def this(user: CurrentUser) = {
		this()

		this.userId = user.apparentId
		this.firstName = user.firstName
		this.lastName = user.lastName
		this.universityId = user.universityId
		this.email = user.email
		this.userType =
			if (user.isStudent) MemberUserType.Student
			else if (user.isStaff) MemberUserType.Staff
			else MemberUserType.Other
	}

	def this(id: String) = {
		this()
		this.universityId = id
	}

	var lastUpdatedDate = DateTime.now

	var missingFromImportSince: DateTime = _

	def fullName: Option[String] = {
		(Option(firstName) ++ Option(lastName)).toList match {
			case Nil => None
			case names => Some(names.mkString(" "))
		}
	}

	def routeName: String = ""
	def officialName = title + " " + Option(fullFirstName).getOrElse(firstName) + " " + lastName

	def description = {
		val userTypeString =
			if (userType == MemberUserType.Staff && Option(jobTitle).isDefined) jobTitle
			else Option(groupName).getOrElse("")

		val deptName = Option(homeDepartment).map(", " + _.name).getOrElse("")

		userTypeString + routeName + deptName
	}

	/**
	 * Get all departments that this member is affiliated to.
	 * (Overriden by StudentMember).
	 */
	def affiliatedDepartments = homeDepartment match {
		case null => Stream()
		case _ => Stream(homeDepartment)
	}

	/**
	 * Get all departments that this student touches. This includes their home department,
	 * the department running their course and any departments that they are taking modules in.
	 *
	 * For each department, enumerate any sub-departments that the member matches
	 */
	def touchedDepartments = {
		def moduleDepts = registeredModulesByYear(None).map(_.department).toStream

		val topLevelDepts = (affiliatedDepartments #::: moduleDepts).distinct
		topLevelDepts flatMap(_.subDepartmentsContaining(this))
	}

	def permissionsParents = touchedDepartments

	/**
	 * Get all modules this this student is registered on, including historically.
	 * TODO consider caching based on getLastUpdatedDate
	 */

	def registeredModulesByYear(year: Option[AcademicYear]) = Set[Module]()
	def moduleRegistrationsByYear(year: Option[AcademicYear]) = Set[ModuleRegistration]()

	def permanentlyWithdrawn = false

	@OneToMany(mappedBy="scope", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	@ForeignKey(name="none")
	@BatchSize(size=200)
	var grantedRoles:JList[MemberGrantedRole] = JArrayList()

	def asSsoUser = {
		val u = new User
		u.setUserId(userId)
		u.setWarwickId(universityId)
		u.setFirstName(firstName)
		u.setLastName(lastName)
		u.setFullName(fullName match {
			case None => "[Unknown user]"
			case Some(name) => name
		})
		u.setEmail(email)
		Option(homeDepartment) map { dept =>
			u.setDepartment(dept.name)
			u.setDepartmentCode(dept.code.toUpperCase)
		}

		u.setLoginDisabled(!(inUseFlag.hasText && (inUseFlag == "Active" || inUseFlag.startsWith("Inactive - Starts "))))
		userType match {
			case MemberUserType.Staff => {
				u.setStaff(true)
				u.setUserType("Staff")
			}
			case MemberUserType.Emeritus => {
				u.setStaff(true)
				u.setUserType("Staff")
			}
			case MemberUserType.Student => {
				u.setStudent(true)
				u.setUserType("Student")
			}
			case _ => u.setUserType("External")
		}

		u.setExtraProperties(JHashMap(
				"urn:websignon:usertype" -> u.getUserType,
				"urn:websignon:timestamp" -> DateTime.now.toString,
				"urn:websignon:usersource" -> "Tabula"
		))

		u.setVerified(true)
		u.setFoundUser(true)
		u
	}

	def toStringProps = Seq(
		"universityId" -> universityId,
		"userId" -> userId,
		"name" -> (firstName + " " + lastName),
		"email" -> email)

	def isStaff = (userType == MemberUserType.Staff)
	def isStudent = (userType == MemberUserType.Student)
	def isRelationshipAgent(relationshipType: StudentRelationshipType) = {
		(userType == MemberUserType.Staff &&
				!relationshipService.listStudentRelationshipsWithMember(
						relationshipType, this
			).isEmpty)
	}

	def hasRelationship(relationshipType: StudentRelationshipType) = false

	def mostSignificantCourseDetails: Option[StudentCourseDetails] = None

	def hasCurrentEnrolment = false

	def isFresh = (missingFromImportSince == null)

	override final def equals(other: Any): Boolean = other match {
		case that: Member =>
			new EqualsBuilder()
				.append(universityId, that.universityId)
				.build()
		case _ => false
	}

	override final def hashCode =
		new HashCodeBuilder()
			.append(universityId)
			.build()
}

@Entity
@DiscriminatorValue("S")
class StudentMember extends Member with StudentProperties {
	this.userType = MemberUserType.Student

	// made this private as can't think of any instances in the app where you wouldn't prefer freshStudentCourseDetails
	@OneToMany(mappedBy = "student", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	@BatchSize(size=200)
	private var studentCourseDetails: JSet[StudentCourseDetails] = JHashSet()

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	def freshStudentCourseDetails = {
		studentCourseDetails.asScala.filter(scd => scd.isFresh)
	}

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	def freshOrStaleStudentCourseDetails = studentCourseDetails.asScala

	@OneToOne
	@JoinColumn(name = "mostSignificantCourse")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var mostSignificantCourse: StudentCourseDetails = _

	def this(id: String) = {
		this()
		this.universityId = id
	}

	/**
	 * Get all departments that this student is affiliated with at a departmental level.
	 * This includes their home department, and the department running their course.
	 */
	override def affiliatedDepartments: Stream[Department] = {
		val sprDepartments = freshStudentCourseDetails.flatMap(scd => Option(scd.department)).toStream
		val sceDepartments = freshStudentCourseDetails.flatMap(_.freshStudentCourseYearDetails).flatMap(scyd => Option(scyd.enrolmentDepartment)).toStream
		val routeDepartments = freshStudentCourseDetails.flatMap(scd => Option(scd.route)).flatMap(route => Option(route.department)).toStream

		(Option(homeDepartment).toStream #:::
				sprDepartments #:::
				sceDepartments #:::
				routeDepartments
		).distinct
	}

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	override def mostSignificantCourseDetails: Option[StudentCourseDetails] = Option(mostSignificantCourse).filter(course => course.isFresh)

	override def hasCurrentEnrolment: Boolean = freshStudentCourseDetails.exists(_.hasCurrentEnrolment)

	override def permanentlyWithdrawn: Boolean = {
		freshStudentCourseDetails
			 .filter(_.statusOnRoute != null)
			 .map(_.statusOnRoute)
			 .filter(_.code != null)
			 .filter(_.code.startsWith("P"))
			 .size == freshStudentCourseDetails.size
	}

	override def hasRelationship(relationshipType: StudentRelationshipType): Boolean =
		studentCourseDetails.asScala.exists(_.hasRelationship(relationshipType))

	override def routeName: String = mostSignificantCourseDetails match {
		case Some(details) =>
			if (details != null && details.route != null) ", " + details.route.name
			else ""
		case _ => ""
	}

	def attachStudentCourseDetails(detailsToAdd: StudentCourseDetails) {
		studentCourseDetails.remove(detailsToAdd)
		studentCourseDetails.add(detailsToAdd)
	}

	override def registeredModulesByYear(year: Option[AcademicYear]): Set[Module] =
		freshStudentCourseDetails.toSet[StudentCourseDetails].flatMap(_.registeredModulesByYear(year))

	override def moduleRegistrationsByYear(year: Option[AcademicYear]): Set[ModuleRegistration] =
		freshStudentCourseDetails.toSet[StudentCourseDetails].flatMap(_.moduleRegistrationsByYear(year))
}

@Entity
@DiscriminatorValue("N")
class StaffMember extends Member with StaffProperties {
	this.userType = MemberUserType.Staff

	def this(id: String) = {
		this()
		this.universityId = id
	}
}

@Entity
@DiscriminatorValue("A")
class EmeritusMember extends Member with StaffProperties {
	this.userType = MemberUserType.Emeritus

	def this(id: String) = {
		this()
		this.universityId = id
	}
}

@Entity
@DiscriminatorValue("O")
class OtherMember extends Member with AlumniProperties {
	this.userType = MemberUserType.Other

	def this(id: String) = {
		this()
		this.universityId = id
	}
}

class RuntimeMember(user: CurrentUser) extends Member(user) {
	override def permissionsParents = Stream.empty
}

trait MemberProperties {
	@Id var universityId: String = _
	def id = universityId

	@Column(nullable = false)
	@Restricted(Array("Profiles.Read.Usercode"))
	var userId: String = _

	var firstName: String = _
	var lastName: String = _
	var email: String = _

	@Restricted(Array("Profiles.Read.HomeEmail"))
	var homeEmail: String = _

	var title: String = _
	var fullFirstName: String = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.MemberUserTypeUserType")
	@Column(insertable = false, updatable = false)
	var userType: MemberUserType = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.GenderUserType")
	var gender: Gender = _

	@OneToOne(fetch = FetchType.LAZY, cascade=Array(ALL))
	@JoinColumn(name = "PHOTO_ID")
	var photo: FileAttachment = _

	var inUseFlag: String = _

	var inactivationDate: LocalDate = _

	var groupName: String = _

	@ManyToOne
	@JoinColumn(name = "home_department_id")
	var homeDepartment: Department = _

	@Restricted(Array("Profiles.Read.DateOfBirth"))
	var dateOfBirth: LocalDate = _

	var jobTitle: String = _

	@Restricted(Array("Profiles.Read.TelephoneNumber"))
	var phoneNumber: String = _

	@Restricted(Array("Profiles.Read.Nationality"))
	var nationality: String = _

	@Restricted(Array("Profiles.Read.MobileNumber"))
	var mobileNumber: String = _
}

trait StudentProperties {
	@OneToOne(cascade = Array(ALL))
	@JoinColumn(name="HOME_ADDRESS_ID")
	@Restricted(Array("Profiles.Read.HomeAddress"))
	var homeAddress: Address = null

	@OneToOne(cascade = Array(ALL))
	@JoinColumn(name="TERMTIME_ADDRESS_ID")
	@Restricted(Array("Profiles.Read.TermTimeAddress"))
	var termtimeAddress: Address = null

	@OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = Array(ALL))
	@Restricted(Array("Profiles.Read.NextOfKin"))
	@BatchSize(size=200)
	var nextOfKins:JList[NextOfKin] = JArrayList()
}

trait StaffProperties {
//	var teachingStaff: JBoolean = _
}

trait AlumniProperties
