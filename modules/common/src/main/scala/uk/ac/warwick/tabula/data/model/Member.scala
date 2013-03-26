package uk.ac.warwick.tabula.data.model
import scala.beans.BeanProperty
import org.hibernate.annotations.{AccessType, FilterDefs, FilterDef, Filters, Filter, Type}
import org.joda.time.DateTime
import org.joda.time.LocalDate
import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model.permissions.MemberGrantedRole
import org.hibernate.annotations.ForeignKey

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
abstract class Member extends MemberProperties with ToString with HibernateVersioned with PermissionsTarget {
	
	@transient 
	var profileService = Wire.auto[ProfileService]
		
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
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var lastUpdatedDate = DateTime.now
	
	def fullName: Option[String] = {
		List(Option(firstName), Option(lastName)).flatten match {
			case Nil => None
			case names => Some(names.mkString(" "))
		}	
	}
	def getFullName = fullName // need this for a def, as reference to fullName within Spring tag requires a getter
	
	def officialName = title + " " + fullFirstName + " " + lastName
	def description = {
		val userTypeString = 
			if (userType == MemberUserType.Staff && Option(jobTitle).isDefined) jobTitle
			else Option(groupName).getOrElse("")
		
		val deptName = Option(homeDepartment).map(", " + _.name).getOrElse("")
		 
		userTypeString + deptName
	}
	
	/** 
	 * Get all departments that this student is affiliated with at a departmental level.
	 * This includes their home department, and the department running their course.
	 */
	def affiliatedDepartments =
		Set(Option(homeDepartment)).flatten.toSeq

	/** 
	 * Get all departments that this student touches. This includes their home department, 
	 * the department running their course and any departments that they are taking modules in.
	 */
	def touchedDepartments = {
		val moduleDepts = registeredModules.map(x => x.department)
		
		(affiliatedDepartments ++ moduleDepts).toSet.toSeq
	}
	
	def permissionsParents = touchedDepartments

	/**
	 * Get all modules this this student is registered on, including historically.
	 * TODO consider caching based on getLastUpdatedDate
	 */
	def registeredModules = {
		profileService.getRegisteredModules(getUniversityId)
	}
	
	@OneToMany(mappedBy="scope", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	@ForeignKey(name="none")
	@BeanProperty var grantedRoles:JList[MemberGrantedRole] = ArrayList()
	
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

			
	def personalTutor: Any = "Not applicable"
	
	def isStaff = (userType == MemberUserType.Staff)
	def isStudent = (userType == MemberUserType.Student)
	def isAPersonalTutor = (userType == MemberUserType.Staff && !profileService.listStudentRelationshipsWithMember(RelationshipType.PersonalTutor, this).isEmpty)
	def hasAPersonalTutor = false
}

@Entity
@DiscriminatorValue("S")
class StudentMember extends Member with StudentProperties with PostLoadBehaviour {
	this.userType = MemberUserType.Student
	
	@OneToOne(fetch = FetchType.LAZY, mappedBy = "student", cascade = Array(ALL))
	var studyDetails: StudyDetails = new StudyDetails
	studyDetails.student = this
	
	def this(id: String) = {
		this()
		this.universityId = id
	}

	def statusString: String = {
		var statusString = ""
		if (studyDetails != null && studyDetails.sprStatus!= null)
			statusString = studyDetails.sprStatus.fullName.toLowerCase()
		if (studyDetails != null && studyDetails.enrolmentStatus != null)
			statusString += " (" + studyDetails.enrolmentStatus.fullName.toLowerCase() + ")"
		statusString
	}
	
	// Find out if the student has an SCE record for the current year (which will mean
	// their study details will be filled in).
	// Could just check that enrolment status is not null, but it's not impossible that 
	// on SITS an enrolment status which doesn't exist in the status table has been
	// entered, in which case we wouldn't be able to populate that field - so checking
	// that route is also not null for good measure.
	
	def hasCurrentEnrolment: Boolean = {
		studyDetails != null && studyDetails.enrolmentStatus != null && studyDetails.getRoute != null
	}

	override def description = {
		val userTypeString = Option(groupName).getOrElse("")
		
		val courseName = Option(studyDetails.route).map(", " + _.name).getOrElse("")
		val deptName = Option(homeDepartment).map(", " + _.name).getOrElse("")
		 
		userTypeString + courseName + deptName
	}
	
	/** 
	 * Get all departments that this student is affiliated with at a departmental level.
	 * This includes their home department, and the department running their course.
	 */
	override def affiliatedDepartments = {
		val affDepts = Set(Option(homeDepartment), 
				Option(studyDetails.studyDepartment),
				Option(studyDetails.route).map(x => x.department)
		)
		
		affDepts.flatten.toSeq
	}
	
	override def personalTutor = 
		profileService.findCurrentRelationship(RelationshipType.PersonalTutor, studyDetails.sprCode) map (rel => rel.agentParsed) match {
			case None => "Not recorded"
			case Some(name: String) => name
			case Some(member: Member) => member
			case other => throw new IllegalArgumentException("Unexpected personal tutor found; " + other)
		}
	
	override def hasAPersonalTutor = profileService.findCurrentRelationship(RelationshipType.PersonalTutor, studyDetails.sprCode).isDefined
	
	// If hibernate sets studyDetails to null, make a new empty studyDetails
	override def postLoad {
		if (studyDetails == null) {
			studyDetails = new StudyDetails
			studyDetails.student = this
		}
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

class RuntimeMember(user: CurrentUser) extends Member(user)

trait MemberProperties {
	@Id @BeanProperty var universityId: String = _
	def id = universityId
	
	@BeanProperty @Column(nullable = false) var userId: String = _
	@BeanProperty var firstName: String = _
	@BeanProperty var lastName: String = _
	@BeanProperty var email: String = _
	
	@BeanProperty var homeEmail: String = _
	
	@BeanProperty var title: String = _
	@BeanProperty var fullFirstName: String = _
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.MemberUserTypeUserType")
	@Column(insertable = false, updatable = false)
	@BeanProperty var userType: MemberUserType = _
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.GenderUserType")
	@BeanProperty var gender: Gender = _
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PHOTO_ID")
	@BeanProperty var photo: FileAttachment = _
	
	@BeanProperty var inUseFlag: String = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var inactivationDate: LocalDate = _
	
	@BeanProperty var groupName: String = _
	
	@ManyToOne
	@JoinColumn(name = "home_department_id")
	@BeanProperty var homeDepartment: Department = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var dateOfBirth: LocalDate = _
	
	@BeanProperty var jobTitle: String = _
	@BeanProperty var phoneNumber: String = _
	
	@BeanProperty var nationality: String = _
	@BeanProperty var mobileNumber: String = _
}

trait StudentProperties {
	@OneToOne(cascade = Array(ALL))
	@JoinColumn(name="HOME_ADDRESS_ID")
	@BeanProperty var homeAddress: Address = null
	
	@OneToOne(cascade = Array(ALL))
	@JoinColumn(name="TERMTIME_ADDRESS_ID")
	@BeanProperty var termtimeAddress: Address = null

	@OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = Array(ALL))
	@BeanProperty var nextOfKins:JList[NextOfKin] = ArrayList()
}

trait StaffProperties {
	@BeanProperty var teachingStaff: JBoolean = _
}

trait AlumniProperties
