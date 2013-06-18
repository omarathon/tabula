package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.{AccessType, Type}
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import org.joda.time.LocalDate
import javax.persistence._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.system.permissions.Restricted
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear

@Entity
class StudentCourseDetails extends StudentCourseProperties with ToString with HibernateVersioned with PermissionsTarget {

	@transient
	var relationshipService = Wire.auto[RelationshipService]

	def this(student: StudentMember, scjCode: String) {
		this()
		this.student = student
		this.scjCode = scjCode
	}

	@Id var scjCode: String = _
	def id = scjCode

	@ManyToOne
	@JoinColumn(name="universityId", referencedColumnName="universityId")
	var student: StudentMember = _

	@OneToMany(mappedBy = "studentCourseDetails", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@Restricted(Array("Profiles.Read.StudentCourseDetails"))
	val studentCourseYearDetails: JList[StudentCourseYearDetails] = JArrayList()

	def toStringProps = Seq(
		"scjCode" -> scjCode,
		"sprCode" -> sprCode)

	def permissionsParents = Option(student).toStream

	def hasCurrentEnrolment: Boolean = {
		!latestStudentCourseYearDetails.enrolmentStatus.code.startsWith("P")
	}

	// FIXME this belongs as a Freemarker macro or helper
	def statusString: String = {
		var statusString = ""
		if (sprStatus!= null) {
			statusString = sprStatus.fullName.toLowerCase().capitalize

			val enrolmentStatus = latestStudentCourseYearDetails.enrolmentStatus

			// if the enrolment status is not null and different to the SPR status, append it:
			if (enrolmentStatus != null
				&& enrolmentStatus.fullName != sprStatus.fullName)
					statusString += " (" + enrolmentStatus.fullName.toLowerCase() + ")"
		}
		statusString
	}

	def latestStudentCourseYearDetails: StudentCourseYearDetails = {
		studentCourseYearDetails.asScala.max
	}

	@Restricted(Array("Profiles.PersonalTutor.Read"))
	def personalTutors =
		relationshipService.findCurrentRelationships(RelationshipType.PersonalTutor, this.sprCode)

	@Restricted(Array("Profiles.Supervisor.Read"))
	def supervisors =
		relationshipService.findCurrentRelationships(RelationshipType.Supervisor, this.sprCode)

	def hasAPersonalTutor = !personalTutors.isEmpty

	def hasSupervisor = !supervisors.isEmpty
}

trait StudentCourseProperties {

	@Column(unique=true)
	var sprCode: String = _
	
	var courseCode: String = _

	@ManyToOne
	@JoinColumn(name = "routeCode", referencedColumnName="code")
	var route: Route = _

	@ManyToOne
	@JoinColumn(name = "deptCode", referencedColumnName="code")
	var department: Department = _

	var awardCode: String = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	var beginDate: LocalDate = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	var endDate: LocalDate = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	var expectedEndDate: LocalDate = _

	var courseYearLength: String = _

	@ManyToOne
	@JoinColumn(name="sprStatusCode", referencedColumnName="code")
	var sprStatus: SitsStatus = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var lastUpdatedDate = DateTime.now

	var mostSignificant: JBoolean = _
}
