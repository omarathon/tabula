package uk.ac.warwick.tabula.data.model

import scala.Option.option2Iterable
import org.joda.time.DateTime
import javax.persistence.Basic
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JInteger
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.system.permissions.Restricted
import reflect.BeanProperty
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.EqualsBuilder
import org.hibernate.annotations.Type
import javax.persistence.Column
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._

@Entity
class StudentCourseYearDetails extends StudentCourseYearProperties
	with GeneratedId with ToString with HibernateVersioned with PermissionsTarget
	with Ordered[StudentCourseYearDetails] {

	def this(studentCourseDetails: StudentCourseDetails, sceSequenceNumber: JInteger,year:AcademicYear) {
		this()
		this.studentCourseDetails = studentCourseDetails
		this.sceSequenceNumber = sceSequenceNumber
		this.academicYear = year
	}

	@ManyToOne
	@JoinColumn(name="scjCode", referencedColumnName="scjCode")
	var studentCourseDetails: StudentCourseDetails = _

	def toStringProps = Seq("studentCourseDetails" -> studentCourseDetails)

	def permissionsParents = Option(studentCourseDetails).toStream

	/**
	 * This is used to calculate StudentCourseDetails.latestStudentCourseYearDetails
	 */
	def compare(that:StudentCourseYearDetails): Int = {
		if (this.academicYear != that.academicYear) {
			this.academicYear.compare(that.academicYear)
		}
		else {
			this.sceSequenceNumber - that.sceSequenceNumber
		}
	}

	def equals(that: StudentCourseYearDetails) = {
		(this.studentCourseDetails == that.studentCourseDetails) && (this.sceSequenceNumber == that.sceSequenceNumber)
	}

	def isFresh = (missingFromImportSince == null)
}

trait StudentCourseYearProperties {

	var sceSequenceNumber: JInteger = _

	@ManyToOne
	@JoinColumn(name="enrolmentStatusCode", referencedColumnName="code")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Status"))
	var enrolmentStatus: SitsStatus = _

	// this is the department from the SCE table in SITS (Student Course Enrolment). It is likely to be the
	// same as the department on the Route table, and on the StudentCourseDetails, but in some cases, e.g. where routes
	// change ownership in different years, this might contain a different department. This indicates the
	// department responsible for administration for the student for this year.
	@ManyToOne
	@JoinColumn(name = "enrolment_department_id")
	var enrolmentDepartment: Department = _

	@ManyToOne
	@JoinColumn(name="modeOfAttendanceCode", referencedColumnName="code")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Status"))
	var modeOfAttendance: ModeOfAttendance = _

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var academicYear: AcademicYear = _

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var yearOfStudy: JInteger = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleRegistrationStatusUserType")
	var moduleRegistrationStatus: ModuleRegistrationStatus = _ // cam_ssn.ssn_mrgs

	var lastUpdatedDate = DateTime.now

	var missingFromImportSince: DateTime = _

	@Column(name="cas_used")
	@Restricted(Array("Profiles.Read.CasUsed"))
	var casUsed: JBoolean = _;

}

class StudentCourseYearKey {
	@BeanProperty
	var scjCode: String = _

	@BeanProperty
	var sceSequenceNumber: JInteger = _

	def this(scjCode: String, sceSequenceNumber: JInteger) = {
		this()
		this.scjCode = scjCode
		this.sceSequenceNumber = sceSequenceNumber
	}

	override final def equals(other: Any): Boolean = other match {
		case that: StudentCourseYearKey =>
			new EqualsBuilder()
				.append(scjCode, that.scjCode)
				.append(sceSequenceNumber, that.sceSequenceNumber)
				.build()
		case _ => false
	}

	override final def hashCode =
		new HashCodeBuilder()
				.append(scjCode)
				.append(sceSequenceNumber)
			.build()
}

