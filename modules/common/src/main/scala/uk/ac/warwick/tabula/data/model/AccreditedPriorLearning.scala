package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type
import org.joda.time.DateTime

import javax.persistence._
import uk.ac.warwick.tabula.{ToString, AcademicYear}
import uk.ac.warwick.tabula.system.permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.JavaImports._

/*
 * scj code, award code and sequence number together make up the key in SITS, and form a unique index in Tabula.
 */

@Entity
@AccessType("field")
class AccreditedPriorLearning() extends GeneratedId	with PermissionsTarget with ToString{
	def this(studentCourseDetails: StudentCourseDetails,
		award: Award,
		sequenceNumber: Int,
		academicYear: AcademicYear,
		cats: java.math.BigDecimal,
		level: Level,
		reason: String) {
		this()
		this.studentCourseDetails = studentCourseDetails
		this.award = award
		this.sequenceNumber = sequenceNumber
		this.academicYear = academicYear
		this.cats = cats
		this.level = level
		this.reason = reason
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="scjCode", referencedColumnName="scjCode")
	@Restricted(Array("AccreditedPriorLearning.Read"))
	var studentCourseDetails: StudentCourseDetails = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="awardCode", referencedColumnName="code")
	@Restricted(Array("AccreditedPriorLearning.Read"))
	var award: Award = null

	var sequenceNumber: JInteger = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Restricted(Array("AccreditedPriorLearning.Read"))
	var academicYear: AcademicYear = null

	@Restricted(Array("AccreditedPriorLearning.Read"))
	var cats: java.math.BigDecimal = null

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="levelCode", referencedColumnName="code")
	@Restricted(Array("AccreditedPriorLearning.Read"))
	var level: Level = _

	@Restricted(Array("AccreditedPriorLearning.Read"))
	var reason: String = null

	@Restricted(Array("ModuleRegistration.Core"))
	var lastUpdatedDate = DateTime.now

	def toStringProps = Seq(
		"scjCode" -> studentCourseDetails.scjCode,
		"awardCode" -> award.code,
		"sequenceNumber" -> sequenceNumber)

	def permissionsParents = Stream(Option(studentCourseDetails)).flatten

}
