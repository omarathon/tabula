package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Type
import org.joda.time.DateTime

import javax.persistence._
import uk.ac.warwick.tabula.{SprCode, AcademicYear}
import uk.ac.warwick.tabula.system.permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import org.apache.commons.lang3.builder.CompareToBuilder
import uk.ac.warwick.tabula.JavaImports.JBigDecimal

/*
 * sprCode, moduleCode, cat score, academicYear and occurrence are a notional key for this table but giving it a generated ID to be
 * consistent with the other tables in Tabula which all have a key that's a single field.  In the db, there should be
 * a unique constraint on the combination of those three.
 */


@Entity
@Access(AccessType.FIELD)
class ModuleRegistration() extends GeneratedId	with PermissionsTarget with Ordered[ModuleRegistration] {

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
	var cats: JBigDecimal = null

	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
	var academicYear: AcademicYear = null

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="scjCode", referencedColumnName="scjCode")
	@Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
	var studentCourseDetails: StudentCourseDetails = _

	@Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
	var assessmentGroup: String = null

	@Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
	var occurrence: String = null

	@Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
	var actualMark: JBigDecimal = null

	@Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
	var actualGrade: String = null

	@Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
	var agreedMark: JBigDecimal = null

	@Restricted(Array("Profiles.Read.ModuleRegistration.Results"))
	var agreedGrade: String = null

	def firstDefinedMark: Option[JBigDecimal] = Seq(Option(agreedMark), Option(actualMark)).flatten.headOption

	@Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleSelectionStatusUserType")
	@Column(name="selectionstatuscode")
	@Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
	var selectionStatus: ModuleSelectionStatus = null // core, option or optional core

	@Restricted(Array("Profiles.Read.ModuleRegistration.Core"))
	var lastUpdatedDate = DateTime.now

	override def toString = studentCourseDetails.scjCode + "-" + module.code + "-" + cats + "-" + AcademicYear.toString

	def permissionsParents = Option(studentCourseDetails).toStream

	override def compare(that: ModuleRegistration): Int =
		new CompareToBuilder()
			.append(studentCourseDetails, that.studentCourseDetails)
			.append(module, that.module)
			.append(cats, that.cats)
			.append(academicYear, that.academicYear)
			.append(occurrence, that.occurrence)
			.build()

	def toSITSCode: String = "%s-%s".format(module.code.toUpperCase, cats.stripTrailingZeros().toPlainString)

}

/**
 * Holds data about an individual student's registration on a single module.
 */
case class UpstreamModuleRegistration(year: String, sprCode: String, seatNumber: String, occurrence: String, sequence: String, moduleCode: String, assessmentGroup: String) {

	def universityId = SprCode.getUniversityId(sprCode)

	// Assessment group membership doesn't vary by sequence
	def differentGroup(other: UpstreamModuleRegistration) =
		year != other.year ||
			occurrence != other.occurrence ||
			moduleCode != other.moduleCode ||
			assessmentGroup != other.assessmentGroup

	/**
	 * Returns UpstreamAssessmentGroups matching the group attributes.
	 */
	def toUpstreamAssessmentGroups(sequences: Seq[String]) = {
		sequences.map(sequence => {
			val g = new UpstreamAssessmentGroup
			g.academicYear = AcademicYear.parse(year)
			g.moduleCode = moduleCode
			g.assessmentGroup = assessmentGroup
			g.sequence = sequence
			// for the NONE group, override occurrence to also be NONE, because we create a single UpstreamAssessmentGroup
			// for each module with group=NONE and occurrence=NONE, and all unallocated students go in there together.
			g.occurrence =
				if (assessmentGroup == AssessmentComponent.NoneAssessmentGroup)
					AssessmentComponent.NoneAssessmentGroup
				else
					occurrence
			g
		})
	}

	/**
	 * Returns an UpstreamAssessmentGroup matching the group attributes, including sequence.
	 */
	def toExactUpstreamAssessmentGroup = {
		val g = new UpstreamAssessmentGroup
		g.academicYear = AcademicYear.parse(year)
		g.moduleCode = moduleCode
		g.assessmentGroup = assessmentGroup
		g.sequence = sequence
		// for the NONE group, override occurrence to also be NONE, because we create a single UpstreamAssessmentGroup
		// for each module with group=NONE and occurrence=NONE, and all unallocated students go in there together.
		g.occurrence =
			if (assessmentGroup == AssessmentComponent.NoneAssessmentGroup)
				AssessmentComponent.NoneAssessmentGroup
			else
				occurrence
		g
	}
}
