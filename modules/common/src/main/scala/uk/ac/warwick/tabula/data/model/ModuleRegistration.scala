package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime

import javax.persistence._
import uk.ac.warwick.tabula.AcademicYear
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type

/*
 * sprCode, moduleCode, cat score and academicYear are a notional key for this table but giving it a generated ID to be
 * consistent with the other tables in Tabula which all have a key that's a single field.  In the db, there should be
 * a unique constraint on the combination of those three.
 */

@Entity
@AccessType("field")
class ModuleRegistration() extends GeneratedId {

	def this(studentCourseDetails: StudentCourseDetails, moduleCode: String, cats: Double, academicYear: AcademicYear) {
		this()
		this.studentCourseDetails = studentCourseDetails
		this.moduleCode = moduleCode
		this.academicYear = academicYear
		this.cats = cats
	}

	var moduleCode: String = null
	var cats: Double = 0

	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	var academicYear: AcademicYear = null

	@Type(`type` = "uk.ac.warwick.tabula.data.model.StudentCourseDetails")
	var studentCourseDetails: StudentCourseDetails = null

	var assessmentGroup: String = null

	@Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleSelectionStatusUserType")
	@Column(name="selectionstatuscode")
	var selectionStatus: ModuleSelectionStatus = null // core, option or optional core

	override def toString = studentCourseDetails.scjCode + "-" + moduleCode + "-" + cats + "-" + AcademicYear

	var lastUpdatedDate = DateTime.now
}
