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

@Entity
class StudentCourseYearDetails extends  StudentCourseYearProperties
	with GeneratedId with ToString with HibernateVersioned with PermissionsTarget {

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
}

trait StudentCourseYearProperties {

	var sceSequenceNumber: JInteger = _

	@ManyToOne
	@JoinColumn(name="code", referencedColumnName="enrolmentStatus")
	var enrolmentStatus: SitsStatus = _

	@ManyToOne
	@JoinColumn(name="modeOfAttendanceCode", referencedColumnName="code")
	var modeOfAttendance: ModeOfAttendance = _

	var yearOfStudy: JInteger = _
	
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var lastUpdatedDate = DateTime.now
}
