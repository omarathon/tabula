package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.{AccessType, FilterDefs, FilterDef, Filters, Filter, Type}
import org.joda.time.DateTime
import org.joda.time.LocalDate
import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.permissions.MemberGrantedRole
import org.hibernate.annotations.ForeignKey
import uk.ac.warwick.tabula.system.permissions.Restricted
import uk.ac.warwick.tabula.services.RelationshipService
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.Logging

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
	 */
	def touchedDepartments = {
		def moduleDepts = registeredModules.map(x => x.department).distinct.toStream

		(affiliatedDepartments #::: moduleDepts).distinct
	}

	def permissionsParents = touchedDepartments

	/**
	 * Get all modules this this student is registered on, including historically.
	 * TODO consider caching based on getLastUpdatedDate
	 */
	def registeredModules = {
		profileService.getRegisteredModules(universityId)
	}

	@OneToMany(mappedBy="scope", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	@ForeignKey(name="none")
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
			u.setDepartmentCode(dept.code)
		}
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
}

@Entity
@DiscriminatorValue("S")
class StudentMember extends Member with StudentProperties {
	this.userType = MemberUserType.Student

	@OneToMany(mappedBy = "student", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var studentCourseDetails: JList[StudentCourseDetails] = JArrayList()

	def this(id: String) = {
		this()
		this.universityId = id
	}

	/**
	 * Get all departments that this student is affiliated with at a departmental level.
	 * This includes their home department, and the department running their course.
	 */
	override def affiliatedDepartments: Stream[Department] = {
		val sprDepartments = studentCourseDetails.asScala.flatMap(scd => Option(scd.department)).toStream
		val routeDepartments = studentCourseDetails.asScala.flatMap(scd => Option(scd.route)).flatMap(route => Option(route.department)).toStream

		(Option(homeDepartment).toStream #:::
				sprDepartments #:::
				routeDepartments
		).distinct
	}

	override def mostSignificantCourseDetails: Option[StudentCourseDetails] = {
		if (studentCourseDetails == null || studentCourseDetails.isEmpty) None
		else studentCourseDetails.asScala.find { details => details.mostSignificant != null && details.mostSignificant }
	}

	override def hasCurrentEnrolment: Boolean = studentCourseDetails.asScala.exists(_.hasCurrentEnrolment)
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
	var nextOfKins:JList[NextOfKin] = JArrayList()
}

trait StaffProperties {
	var teachingStaff: JBoolean = _
}

trait AlumniProperties
