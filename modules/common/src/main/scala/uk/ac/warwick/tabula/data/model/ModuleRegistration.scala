package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type
import org.joda.time.DateTime

import javax.persistence._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.system.permissions._

/*
 * sprCode, moduleCode, cat score and academicYear are a notional key for this table but giving it a generated ID to be
 * consistent with the other tables in Tabula which all have a key that's a single field.  In the db, there should be
 * a unique constraint on the combination of those three.
 */

@Entity
@AccessType("field")
class ModuleRegistration() extends GeneratedId {

	def this(studentCourseDetails: StudentCourseDetails, module: Module, cats: java.math.BigDecimal, academicYear: AcademicYear, occurrence: String) {
		this()
		this.studentCourseDetails = studentCourseDetails
		this.module = module
		this.academicYear = academicYear
		this.cats = cats
		this.occurrence = occurrence
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="moduleCode", referencedColumnName="code")
	@Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
	var module: Module = null

	@Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
	var cats: java.math.BigDecimal = null

	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
	var academicYear: AcademicYear = null

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="scjCode", referencedColumnName="scjCode")
	var studentCourseDetails: StudentCourseDetails = _

	@Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
	var assessmentGroup: String = null

	@Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
	var occurrence: String = null

	@Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
	var agreedMark: java.math.BigDecimal = null

	@Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
	var agreedGrade: String = null

	@Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleSelectionStatusUserType")
	@Column(name="selectionstatuscode")
	@Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
	var selectionStatus: ModuleSelectionStatus = null // core, option or optional core

	override def toString = studentCourseDetails.scjCode + "-" + module.code + "-" + cats + "-" + AcademicYear

	var lastUpdatedDate = DateTime.now

	def permissionsParents = Stream(Option(studentCourseDetails)).flatten

}
