package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type
import org.joda.time.DateTime

import javax.persistence._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.system.permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.helpers.Logging

/*
 * sprCode, moduleCode, cat score and academicYear are a notional key for this table but giving it a generated ID to be
 * consistent with the other tables in Tabula which all have a key that's a single field.  In the db, there should be
 * a unique constraint on the combination of those three.
 */

@Entity
@AccessType("field")
class ModuleRegistration() extends GeneratedId	with PermissionsTarget with Logging {

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
	@Restricted(Array("ModuleRegistration.Core"))
	var module: Module = null

	@Restricted(Array("ModuleRegistration.Core"))
	var cats: java.math.BigDecimal = null

	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Restricted(Array("ModuleRegistration.Core"))
	var academicYear: AcademicYear = null

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="scjCode", referencedColumnName="scjCode")
	@Restricted(Array("ModuleRegistration.Core"))
	var studentCourseDetails: StudentCourseDetails = _

	@Restricted(Array("ModuleRegistration.Core"))
	var assessmentGroup: String = null

	@Restricted(Array("ModuleRegistration.Core"))
	var occurrence: String = null

	@Restricted(Array("ModuleRegistration.Results"))
	var agreedMark: java.math.BigDecimal = null

	@Restricted(Array("ModuleRegistration.Results"))
	var agreedGrade: String = null

	@Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleSelectionStatusUserType")
	@Column(name="selectionstatuscode")
	@Restricted(Array("ModuleRegistration.Core"))
	var selectionStatus: ModuleSelectionStatus = null // core, option or optional core

	@Restricted(Array("ModuleRegistration.Core"))
	var lastUpdatedDate = DateTime.now

	override def toString = studentCourseDetails.scjCode + "-" + module.code + "-" + cats + "-" + AcademicYear.toString

	def permissionsParents = Stream(Option(studentCourseDetails)).flatten

}
