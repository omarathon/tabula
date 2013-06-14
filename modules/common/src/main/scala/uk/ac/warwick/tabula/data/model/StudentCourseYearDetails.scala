package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Type
import org.joda.time.DateTime

import javax.persistence._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Entity
class StudentCourseYearDetails extends  StudentCourseYearProperties
	with GeneratedId with ToString with HibernateVersioned with PermissionsTarget
	with Ordered[StudentCourseYearDetails]{

	def this(studentCourseDetails: StudentCourseDetails, sceSequenceNumber: JInteger) {
		this()
		this.studentCourseDetails = studentCourseDetails
		this.sceSequenceNumber = sceSequenceNumber
	}

	@ManyToOne
	@JoinColumn(name="scjCode", referencedColumnName="scjCode")
	var studentCourseDetails: StudentCourseDetails = _
	def studentCourseDetails_ = studentCourseDetails

	def toStringProps = Seq("studentCourseDetails" -> studentCourseDetails)

	def permissionsParents = Option(studentCourseDetails).toStream

	def compare(that:StudentCourseYearDetails): Int = {
		if (this.academicYear != that.academicYear) {
			this.academicYear.compare(that.academicYear)
		}
		else {
			this.sceSequenceNumber - that.sceSequenceNumber
		}
	}
}

trait StudentCourseYearProperties {

	var sceSequenceNumber: JInteger = _

	@ManyToOne
	@JoinColumn(name="code", referencedColumnName="enrolmentStatus")
	var enrolmentStatus: SitsStatus = _

	@ManyToOne
	@JoinColumn(name="modeOfAttendanceCode", referencedColumnName="code")
	var modeOfAttendance: ModeOfAttendance = _

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	var academicYear: AcademicYear = _

	var yearOfStudy: JInteger = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var lastUpdatedDate = DateTime.now

}
