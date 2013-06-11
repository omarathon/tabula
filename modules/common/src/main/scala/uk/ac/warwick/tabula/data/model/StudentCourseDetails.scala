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
class StudentCourseDetails extends StudentCourseProperties with ToString with HibernateVersioned with PermissionsTarget {

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
	def student_ = student

	def toStringProps = Seq(
		"scjCode" -> scjCode,
		"sprCode" -> sprCode)

	def permissionsParents = Option(student).toStream
}

trait StudentCourseProperties {

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
}
