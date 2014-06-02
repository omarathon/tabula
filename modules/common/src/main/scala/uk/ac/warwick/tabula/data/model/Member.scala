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
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.permissions.MemberGrantedRole
import uk.ac.warwick.tabula.system.permissions.{RestrictionProvider, Restricted}
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
import org.hibernate.annotations.Type
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.Filter
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringCheckpointTotal

object Member {
	final val StudentsOnlyFilter = "studentsOnly"
	final val ActiveOnlyFilter = "activeOnly"
	final val FreshOnlyFilter = "freshMemberOnly"
}

/**
 * Notes about the notDeleted filter:
 * filters don't run on session.get() but getById will check for you.
 * queries will only include it if it's the entity after "from" and not
 * some other secondary entity joined on. It's usually possible to flip the
 * query around to make this work.
 */
@FilterDefs(Array(
		new FilterDef(name = Member.StudentsOnlyFilter, defaultCondition = "usertype = 'S'"),
		new FilterDef(name = Member.ActiveOnlyFilter, defaultCondition = "(inuseflag = 'Active' or inuseflag like 'Inactive - Starts %')"),
		new FilterDef(name = Member.FreshOnlyFilter, defaultCondition = "missingFromImportSince is null")
	))
	@Filters(Array(
		new Filter(name = Member.StudentsOnlyFilter),
		new Filter(name = Member.ActiveOnlyFilter),
		new Filter(name = Member.FreshOnlyFilter)
	))
@Entity
@AccessType("field")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
		name="userType",
		discriminatorType=DiscriminatorType.STRING
)
abstract class Member extends MemberProperties with ToString with HibernateVersioned with PermissionsTarget with Logging with Serializable {

	@transient
	var profileService = Wire[ProfileService with StaffAssistantsHelpers]

	@transient
	var relationshipService = Wire[RelationshipService]

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

		val deptName = Option(homeDepartment).fold("")(", " + _.name)

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
	 * Get all departments that this member touches.
	 * (Overridden by StudentMember).
	 */
	def touchedDepartments = {
		val topLevelDepts = affiliatedDepartments
		topLevelDepts flatMap(_.subDepartmentsContaining(this))
	}

	def permissionsParents: Stream[PermissionsTarget] = touchedDepartments
	override def humanReadableId = fullName.getOrElse(toString())
	override final def urlCategory = "member"

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
			case MemberUserType.Staff =>
				u.setStaff(true)
				u.setUserType("Staff")
			case MemberUserType.Emeritus =>
				u.setStaff(true)
				u.setUserType("Staff")
			case MemberUserType.Student =>
				u.setStudent(true)
				u.setUserType("Student")
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

	def isStaff = userType == MemberUserType.Staff
	def isStudent = userType == MemberUserType.Student
	def isRelationshipAgent(relationshipType: StudentRelationshipType) = {
		!relationshipService.listStudentRelationshipsWithMember(relationshipType, this).isEmpty
	}

	// Overridden in StudentMember
	def hasRelationship(relationshipType: StudentRelationshipType) = false

	def isFresh = missingFromImportSince == null

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
	private val studentCourseDetails: JSet[StudentCourseDetails] = JHashSet()

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	def freshStudentCourseDetails = {
		studentCourseDetails.asScala.filter(scd => scd.isFresh).toSeq.sorted
	}

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	def freshOrStaleStudentCourseDetails = studentCourseDetails.asScala

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	def freshOrStaleStudentCourseYearDetails(year: AcademicYear) =
		freshOrStaleStudentCourseDetails
			.flatMap(_.freshOrStaleStudentCourseYearDetails)
			.filter(_.academicYear == year)

	@OneToOne(fetch = FetchType.LAZY) // don't cascade, cascaded separately
	@JoinColumn(name = "mostSignificantCourse")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var mostSignificantCourse: StudentCourseDetails = _

	@Restricted(Array("Profiles.Read.Tier4VisaRequirement"))
	def casUsed: Option[Boolean] = {
			mostSignificantCourseDetails.flatMap(scd => scd.latestStudentCourseYearDetails.casUsed match {
				case null => None
				case casUsed => Some(casUsed)
			})
	}

	@Restricted(Array("Profiles.Read.Tier4VisaRequirement"))
	def hasTier4Visa: Option[Boolean] = {
		mostSignificantCourseDetails.flatMap(scd => scd.latestStudentCourseYearDetails.tier4Visa match {
			case null => None
			case hasTier4Visa => Some(hasTier4Visa)
		})
	}

	@OneToMany(mappedBy = "student", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@Restricted(Array("MonitoringPoints.View"))
	@BatchSize(size=200)
	var attendanceCheckpointTotals: JSet[AttendanceMonitoringCheckpointTotal] = JHashSet()

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

	/**
	 * Get all departments that this student touches. This includes their home department,
	 * the department running their course and any departments that they are taking modules in.
	 *
	 * For each department, enumerate any sub-departments that the member matches
	 */
	override def touchedDepartments = {
		def moduleDepts = registeredModulesByYear(None).map(_.department).toStream

		val topLevelDepts = (affiliatedDepartments #::: moduleDepts).distinct
		topLevelDepts flatMap(_.subDepartmentsContaining(this))
	}

	@transient var smallGroupService = Wire[SmallGroupService]

	/**
	 * Get all small groups that this student is signed up for
	 */
	def registeredSmallGroups: Stream[SmallGroup] = smallGroupService.findSmallGroupsByStudent(asSsoUser).toStream

	override def permissionsParents: Stream[PermissionsTarget] = {
		val departments: Stream[PermissionsTarget] = touchedDepartments
		val smallGroups: Stream[PermissionsTarget] = registeredSmallGroups
		val currentRoute: Stream[PermissionsTarget] = mostSignificantCourseDetails.map { _.route }.toStream

		departments #::: smallGroups #::: currentRoute
	}

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	def mostSignificantCourseDetails: Option[StudentCourseDetails] = Option(mostSignificantCourse).filter(course => course.isFresh)

	def defaultYearDetails: Option[StudentCourseYearDetails] = mostSignificantCourseDetails.map(_.latestStudentCourseYearDetails)

	def hasCurrentEnrolment: Boolean = freshStudentCourseDetails.exists(_.hasCurrentEnrolment)

	// perm withdrawn if the member is inactive or
	// there are no fresh studentCourseDetails with a non-null route status that's not P
	def permanentlyWithdrawn: Boolean = {
		inUseFlag.matches("Inactive.*") ||
			(freshStudentCourseDetails.count(_.statusOnRoute == null) == 0 && // if they have a course details with null status they may be active
				freshStudentCourseDetails
					.filter(_.statusOnRoute != null)
					.map(_.statusOnRoute)
					.filter(_.code != null)
					.count(!_.code.startsWith("P")) == 0
			)
	}

/*	def permanentlyWithdrawn: Boolean = {
		inUseFlag.matches("Inactive.*") ||
			freshStudentCourseDetails
				.filter(_.statusOnRoute != null)
				.map(_.statusOnRoute)
				.filter(_.code != null)
				.filter(!_.code.startsWith("P"))
				.size == 0
	}	*/

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

	/**
	 * Get all modules this this student is registered on, including historically.
	 * TODO consider caching based on getLastUpdatedDate
	 */
	def registeredModulesByYear(year: Option[AcademicYear]): Set[Module] =
		freshStudentCourseDetails.toSet[StudentCourseDetails].flatMap(_.registeredModulesByYear(year))

	def moduleRegistrationsByYear(year: Option[AcademicYear]): Set[ModuleRegistration] =
		freshStudentCourseDetails.toSet[StudentCourseDetails].flatMap(_.moduleRegistrationsByYear(year))

	def isPGR = groupName == "Postgraduate (research) FT" || groupName == "Postgraduate (research) PT"
}

@Entity
@DiscriminatorValue("N")
class StaffMember extends Member with StaffProperties {
	this.userType = MemberUserType.Staff

	def this(id: String) = {
		this()
		this.universityId = id
	}

	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "assistantsgroup_id")
	private var _assistantsGroup: UserGroup = UserGroup.ofUsercodes
	def assistants: UnspecifiedTypeUserGroup = new UserGroupCacheManager(_assistantsGroup, profileService.staffAssistantsHelper)
	def assistants_=(group: UserGroup) { _assistantsGroup = group }
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
class OtherMember extends Member with AlumniProperties with RestrictedPhoneNumber {
	this.userType = MemberUserType.Other

	def this(id: String) = {
		this()
		this.universityId = id
	}
}

class RuntimeMember(user: CurrentUser) extends Member(user) with RestrictedPhoneNumber {
	override def permissionsParents = Stream.empty
}

trait MemberProperties extends StringId {

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "home_department_id")
	var homeDepartment: Department = _

	@Restricted(Array("Profiles.Read.DateOfBirth"))
	var dateOfBirth: LocalDate = _

	var jobTitle: String = _

	@RestrictionProvider("phoneNumberPermissions")
	var phoneNumber: String = _

	@Restricted(Array("Profiles.Read.Nationality"))
	var nationality: String = _

	@Restricted(Array("Profiles.Read.MobileNumber"))
	var mobileNumber: String = _

	@Column(name = "timetable_hash")
	var timetableHash: String = _

	def phoneNumberPermissions: Seq[Permission]

}

trait StudentProperties extends RestrictedPhoneNumber {
	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name="HOME_ADDRESS_ID")
	@Restricted(Array("Profiles.Read.HomeAddress"))
	var homeAddress: Address = null

	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name="TERMTIME_ADDRESS_ID")
	@Restricted(Array("Profiles.Read.TermTimeAddress"))
	var termtimeAddress: Address = null

	@OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = Array(ALL))
	@Restricted(Array("Profiles.Read.NextOfKin"))
	@BatchSize(size=200)
	var nextOfKins:JList[NextOfKin] = JArrayList()

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "disability")
	@Restricted(Array("Profiles.Read.Disability"))
	var disability: Disability = _

	@Column(name="tier4_visa_requirement")
	@Restricted(Array("Profiles.Read.Tier4VisaRequirement"))
	var tier4VisaRequirement: JBoolean = _
}

trait RestrictedPhoneNumber {
	def phoneNumberPermissions = Seq(Permissions.Profiles.Read.TelephoneNumber)
}

trait StaffProperties {
	// Anyone can view staff phone number
	def phoneNumberPermissions = Nil
}

trait AlumniProperties
