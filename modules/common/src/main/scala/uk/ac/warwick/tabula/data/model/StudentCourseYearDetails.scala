package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import javax.persistence.Basic
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.system.permissions.Restricted
import scala.beans.BeanProperty
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.EqualsBuilder
import org.hibernate.annotations.{Filter, Filters, FilterDef, FilterDefs, Type}
import javax.persistence.Column
import uk.ac.warwick.tabula.JavaImports._
import javax.persistence.FetchType

object StudentCourseYearDetails {
	final val FreshCourseYearDetailsOnlyFilter = "freshStudentCourseYearDetailsOnly"
}

@FilterDefs(Array(
	new FilterDef(name = StudentCourseYearDetails.FreshCourseYearDetailsOnlyFilter, defaultCondition = "missingFromImportSince is null")
))
@Filters(Array(
	new Filter(name = StudentCourseYearDetails.FreshCourseYearDetailsOnlyFilter)
))
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="scjCode", referencedColumnName="scjCode")
	var studentCourseDetails: StudentCourseDetails = _

	def toStringProps = Seq("studentCourseDetails" -> studentCourseDetails, "sceSequenceNumber" -> sceSequenceNumber, "academicYear" -> academicYear)

	def permissionsParents = Option(studentCourseDetails).toStream

	/**
	 * This is used to calculate StudentCourseDetails.latestStudentCourseYearDetails
	 */
	def compare(that:StudentCourseYearDetails): Int = {
		if (this.studentCourseDetails.scjCode != that.studentCourseDetails.scjCode)
			this.studentCourseDetails.compare(that.studentCourseDetails)
		else if (this.academicYear != that.academicYear)
			this.academicYear.compare(that.academicYear)
		else
			this.sceSequenceNumber - that.sceSequenceNumber
	}

	def equals(that: StudentCourseYearDetails) = {
		(this.studentCourseDetails.scjCode == that.studentCourseDetails.scjCode) && (this.sceSequenceNumber == that.sceSequenceNumber)
	}

	def isFresh = (missingFromImportSince == null)

	// There can be more than one StudentCourseYearDetails per year if there are multiple sequence numbers,
	// so moduleRegistrations are not attached directly - instead, get them from StudentCourseDetails,
	// filtering by year:
	def moduleRegistrations = studentCourseDetails.moduleRegistrations.filter(_.academicYear == this.academicYear)

	// similarly for accredited prior learning
	def accreditedPriorLearning = {
		studentCourseDetails.accreditedPriorLearning.filter(_.academicYear == this.academicYear)
	}

	def registeredModules = moduleRegistrations.map(mr => mr.module)

	def hasModuleRegistrations = !moduleRegistrations.isEmpty

	def hasModuleRegistrationWithNonStandardOccurrence = moduleRegistrations.exists(_.occurrence != "A")

	def hasAccreditedPriorLearning = !accreditedPriorLearning.isEmpty

	def isLatest = this.equals(studentCourseDetails.latestStudentCourseYearDetails)
}

trait BasicStudentCourseYearProperties {
	var sceSequenceNumber: JInteger = _

	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var yearOfStudy: JInteger = _

	@Column(name="cas_used")
	@Restricted(Array("Profiles.Read.Tier4VisaRequirement"))
	var casUsed: JBoolean = _

	@Column(name="tier4visa")
	@Restricted(Array("Profiles.Read.Tier4VisaRequirement"))
	var tier4Visa: JBoolean = _

}

trait StudentCourseYearProperties extends BasicStudentCourseYearProperties {
	var lastUpdatedDate = DateTime.now
	var missingFromImportSince: DateTime = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleRegistrationStatusUserType")
	var moduleRegistrationStatus: ModuleRegistrationStatus = _ // cam_ssn.ssn_mrgs

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Core"))
	var academicYear: AcademicYear = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="enrolmentStatusCode", referencedColumnName="code")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Status"))
	var enrolmentStatus: SitsStatus = _

	// this is the department from the SCE table in SITS (Student Course Enrolment). It is likely to be the
	// same as the department on the Route table, and on the StudentCourseDetails, but in some cases, e.g. where routes
	// change ownership in different years, this might contain a different department. This indicates the
	// department responsible for administration for the student for this year.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "enrolment_department_id")
	var enrolmentDepartment: Department = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="modeOfAttendanceCode", referencedColumnName="code")
	@Restricted(Array("Profiles.Read.StudentCourseDetails.Status"))
	var modeOfAttendance: ModeOfAttendance = _
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

