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
import scala.collection.JavaConverters._

@Entity
class StudentCourseDetails
	extends StudentCourseProperties
	with ToString
	with HibernateVersioned
	with PermissionsTarget
	with Ordered[StudentCourseDetails] {

	@transient
	var relationshipService = Wire.auto[RelationshipService]

	def this(student: StudentMember, scjCode: String) {
		this()
		this.student = student
		this.scjCode = scjCode
	}

	@Id var scjCode: String = _
	def id = scjCode
	def urlSafeId = scjCode.replace("/", "_")

	@ManyToOne
	@JoinColumn(name="universityId", referencedColumnName="universityId")
	var student: StudentMember = _

	@OneToMany(mappedBy = "studentCourseDetails", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	val studentCourseYearDetails: JList[StudentCourseYearDetails] = JArrayList()

	@OneToMany(mappedBy = "studentCourseDetails", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var moduleRegistrations: JList[ModuleRegistration] = JArrayList()

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

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	def latestStudentCourseYearDetails: StudentCourseYearDetails =
		studentCourseYearDetails.asScala.max

	def courseType = CourseType.fromCourseCode(course.code)

	// We can't restrict this because it's not a getter. Restrict in
	// view code if necessary (or implement for all methods in  ScalaBeansWrapper)
	def relationships(relationshipType: StudentRelationshipType) =
		relationshipService.findCurrentRelationships(relationshipType, this.sprCode)

	def hasRelationship(relationshipType: StudentRelationshipType) = !relationships(relationshipType).isEmpty

	def compare(that:StudentCourseDetails): Int = {
		this.scjCode.compare(that.scjCode)
	}

	def equals(that:StudentCourseDetails) = this.scjCode == that.scjCode

	def attachStudentCourseYearDetails(yearDetailsToAdd: StudentCourseYearDetails) {
		studentCourseYearDetails.remove(yearDetailsToAdd)
		studentCourseYearDetails.add(yearDetailsToAdd)
	}

	def hasModuleRegistrations = {
		!moduleRegistrations.isEmpty()
	}
}

trait StudentCourseProperties {
	var sprCode: String = _

	@ManyToOne
	@JoinColumn(name = "courseCode", referencedColumnName="code")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var course: Course = _

	@ManyToOne
	@JoinColumn(name = "routeCode", referencedColumnName="code")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var route: Route = _

	// this is the department from the SPR table in SITS (Student Programme Route).  It is likely to be the
	// same as the department on the Route table, but in some cases, e.g. where routes change ownership in
	// different years, the SPR code might contain a different department.
	@ManyToOne
	@JoinColumn(name = "department_id")
	var department: Department = _

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var awardCode: String = _

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var levelCode: String = _

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var beginDate: LocalDate = _

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var endDate: LocalDate = _

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var expectedEndDate: LocalDate = _

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var courseYearLength: String = _

	@ManyToOne
	@JoinColumn(name="sprStatusCode")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Status"))
	var sprStatus: SitsStatus = _

	var lastUpdatedDate = DateTime.now

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var mostSignificant: JBoolean = _
}

sealed abstract class CourseType(val code: String, val level: String, val description: String, val courseCodeChar: Char)

object CourseType {
	case object PGR extends CourseType("PG(R)", "Postgraduate", "Postgraduate (Research)", 'R')
	case object PGT extends CourseType("PG(T)", "Postgraduate", "Postgraduate (Taught)", 'T')
	case object UG extends CourseType("UG", "Undergraduate", "Undergraduate", 'U')
	case object Foundation extends CourseType("F", "Foundation", "Foundation course", 'F')
	case object PreSessional extends CourseType("PS", "Pre-sessional", "Pre-sessional course", 'N')

	def fromCourseCode(cc: String): CourseType = {
		if (cc.isEmpty) null
		cc.charAt(0) match {
			case UG.courseCodeChar => UG
			case PGT.courseCodeChar => PGT
			case PGR.courseCodeChar => PGR
			case Foundation.courseCodeChar => Foundation
			case PreSessional.courseCodeChar => PreSessional
			case other => throw new IllegalArgumentException("Unexpected first character of course code: %s".format(other))
		}
	}
}
