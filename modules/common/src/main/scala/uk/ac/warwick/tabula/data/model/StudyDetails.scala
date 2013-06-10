package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.{AccessType, Type}
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import org.joda.time.LocalDate
import javax.persistence._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.permissions.PermissionsTarget


@Entity
@AccessType("field")
class StudyDetails extends StudyDetailsProperties with ToString with HibernateVersioned with PermissionsTarget {

	@OneToOne(fetch = FetchType.LAZY)
	@PrimaryKeyJoinColumn
	var student: StudentMember = _

	@GenericGenerator(name = "generator", strategy = "foreign", parameters = Array(new Parameter(name="property", value = "student")))
	@GeneratedValue(generator = "generator")
	@Id var universityId: String = _

	def id = universityId

	def toStringProps = Seq("student" -> student)

	def permissionsParents = Option(student).toStream

}

trait StudyDetailsProperties {
	
	// WARNING: if and when we @Restrict these properties, we should NOT @Restrict sprCode as it is an identifier

	var sprCode: String = _
	var scjCode: String = _
	var sitsCourseCode: String = _

	@ManyToOne
	@JoinColumn(name = "route_id")
	var route: Route = _

	@ManyToOne
	@JoinColumn(name = "study_department_id")
	var studyDepartment: Department = _

	var yearOfStudy: JInteger = _

	var fundingSource: String = _

	var intendedAward: String = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	var beginDate: LocalDate = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	var endDate: LocalDate = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	var expectedEndDate: LocalDate = _

	var courseYearLength: String = _

	//var sprStatusCode: String = _
	@ManyToOne
	@JoinColumn(name="sprStatusCode", referencedColumnName="code")
	var sprStatus: SitsStatus = _

	//var enrolmentStatusCode: String = _
	@ManyToOne
	@JoinColumn(name="enrolmentStatusCode", referencedColumnName="code")
	var enrolmentStatus: SitsStatus = _


	//var levelCode: String = _

	@ManyToOne
	@JoinColumn(name="modeOfAttendanceCode", referencedColumnName="code")
	var modeOfAttendance: ModeOfAttendance = _

//	var attendanceMode: String = _
//
//	var programmeOfStudy: String = _
//
//	@Basic
//	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
//	var academicYear: AcademicYear = _
//
//	@Basic
//	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
//	var courseStartYear: AcademicYear = _
//
//	@Basic
//	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
//	var yearCommencedDegree: AcademicYear = _
//
//	@Basic
//	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
//	var courseBaseYear: AcademicYear = _
//
//	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
//	var courseEndDate: LocalDate = _

//	var transferReason: String = _

//
//	var feeStatus: String = _
//	var domicile: String = _
//	var highestQualificationOnEntry: String = _
//
//	var lastInstitute: String = _
//	var lastSchool: String = _

}