package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type
import org.joda.time.DateTime

import javax.persistence._
import uk.ac.warwick.tabula.AcademicYear

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

	@ManyToOne
	@JoinColumn(name="moduleCode", referencedColumnName="code")
	var module: Module = null
	var cats: java.math.BigDecimal = null

	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	var academicYear: AcademicYear = null

	@ManyToOne
	@JoinColumn(name="scjCode", referencedColumnName="scjCode")
	var studentCourseDetails: StudentCourseDetails = _

	var assessmentGroup: String = null

	var occurrence: String = null

	@Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleSelectionStatusUserType")
	@Column(name="selectionstatuscode")
	var selectionStatus: ModuleSelectionStatus = null // core, option or optional core

	override def toString = studentCourseDetails.scjCode + "-" + module.code + "-" + cats + "-" + AcademicYear

	var lastUpdatedDate = DateTime.now
}
