package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime

import javax.persistence._
import uk.ac.warwick.tabula.AcademicYear
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type

/*
 * sprCode, moduleCode and academicYear are a notional key for this table but giving it a generated ID to be
 * consistent with the other tables in Tabula which all have a key that's a single field.  In the db, there should be
 * a unique constraint on the combination of those three.
 */

@Entity
@AccessType("field")
class ModuleRegistration() extends GeneratedId {

	def this(sprCode: String, moduleCode: String, academicYear: AcademicYear, cats: Double) {
		this()
		this.sprCode = sprCode
		this.moduleCode = moduleCode
		this.academicYear = academicYear
		this.cats = cats
	}

	var sprCode: String = null
	var moduleCode: String = null
	var cats: Double = 0

	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	var academicYear: AcademicYear = null

	var assessmentGroup: String = null

	// NB - an "optional core" is a core which does not need to be taken in a certain year, e.g. for part-time students
	// This is "SES_CODE" in SITS
	var selectionStatusCode: String = null // C for core, O for option, CO for optional core

	override def toString = sprCode + "-" + moduleCode + "-" + cats + "-" + AcademicYear

	var lastUpdatedDate = DateTime.now
}
