package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.Id
import uk.ac.warwick.tabula.AcademicYear

/*
 * sprCode, moduleCode and academicYear are a notional key for this table but giving it a generated ID to be
 * consistent with the other tables in Tabula which all have a key that's a single field.  In the db, there should be
 * a unique constraint on the combination of those three.
 */

@Entity
class ModuleRegistration(val sprCode: String, val moduleCode: String, val academicYear: AcademicYear) extends GeneratedId {

	var cats: String = _
	var assessmentGroup: String = _

	// NB - an "optional core" is a core which does not need to be taken in a certain year, e.g. for part-time students
	// This is "SES_CODE" in SITS
	var selectionStatusCode: String = _ // C for core, O for option, CO for optional core

	override def toString = sprCode + "/" + moduleCode + "/" + academicYear

	var lastUpdatedDate = DateTime.now

}
