package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations._
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
import javax.persistence.Entity
import javax.persistence.CascadeType
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.convert.ConvertibleConverter

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

	// made this private as can't think of any instances in the app where you wouldn't prefer freshStudentCourseDetails
	@OneToMany(mappedBy = "studentCourseDetails", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	@BatchSize(size=200)
	private val studentCourseYearDetails: JList[StudentCourseYearDetails] = JArrayList()

	def freshStudentCourseYearDetails = studentCourseYearDetails.asScala.filter(scyd => scyd.isFresh)
	def freshOrStaleStudentCourseYearDetails = studentCourseYearDetails.asScala

	@OneToMany(mappedBy = "studentCourseDetails", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	@BatchSize(size=200)
	var moduleRegistrations: JList[ModuleRegistration] = JArrayList()

	def registeredModulesByYear(year: Option[AcademicYear]): Seq[Module] = moduleRegistrationsByYear(year).map(_.module)

	def moduleRegistrationsByYear(year: Option[AcademicYear]): Seq[ModuleRegistration] =
		moduleRegistrations.asScala.collect {
			case modReg if year.isEmpty => modReg
			case modReg if modReg.academicYear == year.getOrElse(null) => modReg
	}

	def toStringProps = Seq(
		"scjCode" -> scjCode,
		"sprCode" -> sprCode)

	def permissionsParents = Stream(Option(student), Option(route)).flatten

	def hasCurrentEnrolment: Boolean = {
		Option(latestStudentCourseYearDetails).map { scyd =>
			!scyd.enrolmentStatus.code.startsWith("P")
		}.getOrElse(false)
	}

	// The reason this method isn't on SitsStatus is that P* can have a meaning other than
	// permanently withdrawn in the context of applicants, but not in the context of
	// the student's route status (sprStatus)
	def permanentlyWithdrawn = {
		sprStatus != null && sprStatus.code.startsWith("P")
	}

	@OneToOne
	@JoinColumn(name = "latestYearDetails")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var latestStudentCourseYearDetails: StudentCourseYearDetails = _

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

		latestStudentCourseYearDetails = freshStudentCourseYearDetails.max
	}

	def hasModuleRegistrations = {
		!moduleRegistrations.isEmpty()
	}

	def isFresh = (missingFromImportSince == null)

	def addStudentCourseYearDetails(scyd: StudentCourseYearDetails) = studentCourseYearDetails.add(scyd)

	def removeStudentCourseYearDetails(scyd: StudentCourseYearDetails) = studentCourseYearDetails.remove(scyd)

	def beginYear = beginDate match {
		case begin: LocalDate => new Integer(beginDate.year().getAsText)
		case null => new Integer(0)
	}

	def endYear = endDate match {
		case null =>
			expectedEndDate match {
				case expectedEnd: LocalDate => new Integer(expectedEndDate.year().getAsText)
				case null => new Integer(0)
			}
		case end: LocalDate => new Integer(end.year().getAsText)
	}
}

trait StudentCourseProperties {
	// There can be multiple StudentCourseDetails rows for a single SPR code, even though a route is a sub-category of a course;
	// this is just an artefact of the weird way SITS works.  If a student changes route within a course, they end up with a new
	// course join (SCJ) row in SITS.  Equally perversely, they keep the same sprcode and SPR row even though this should be the
	// student's record for their route (SPR = student programme route) - the route code is just edited.  Hence this is not unique.
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

	var missingFromImportSince: DateTime = _

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var mostSignificant: JBoolean = _
}

sealed abstract class CourseType(val code: String, val level: String, val description: String, val courseCodeChar: Char) extends Convertible[String] {
	def value = code
}

object CourseType {
	implicit val factory = { code: String => CourseType(code) }

	case object PGR extends CourseType("PG(R)", "Postgraduate", "Postgraduate (Research)", 'R')
	case object PGT extends CourseType("PG(T)", "Postgraduate", "Postgraduate (Taught)", 'T')
	case object UG extends CourseType("UG", "Undergraduate", "Undergraduate", 'U')
	case object Foundation extends CourseType("F", "Foundation", "Foundation course", 'F')
	case object PreSessional extends CourseType("PS", "Pre-sessional", "Pre-sessional course", 'N')

	val all = Seq(UG, PGT, PGR, Foundation, PreSessional)

	def apply(code: String): CourseType = code match {
		case UG.code => UG
		case PGT.code => PGT
		case PGR.code => PGR
		case Foundation.code => Foundation
		case PreSessional.code => PreSessional
		case other => throw new IllegalArgumentException("Unexpected course code: %s".format(other))
	}

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

// converter for spring
class CourseTypeConverter extends ConvertibleConverter[String, CourseType]