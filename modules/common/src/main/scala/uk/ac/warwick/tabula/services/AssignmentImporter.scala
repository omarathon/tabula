package uk.ac.warwick.tabula.services

import java.sql.ResultSet
import java.sql.Types
import collection.JavaConversions._
import org.joda.time.DateTime
import org.springframework.beans.factory.InitializingBean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.jdbc.core.SqlParameter
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters
import org.springframework.stereotype.Service
import javax.annotation.Resource
import javax.sql.DataSource
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.FunctionConversions._
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.data.model.UpstreamAssignment
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.SprCode
import java.sql.Connection
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.ac.warwick.tabula.data.model.Member

@Service
class AssignmentImporter extends InitializingBean {
	import AssignmentImporter._

	@Resource(name = "academicDataStore") var ads: DataSource = _
	var assessmentGroupMemberQuery: AssessmentGroupMembersQuery = _
	var upstreamAssessmentGroupQuery: UpstreamAssessmentGroupQuery = _
	var upstreamAssignmentQuery: UpstreamAssignmentQuery = _
	var jdbc: NamedParameterJdbcTemplate = _

	override def afterPropertiesSet {
		assessmentGroupMemberQuery = new AssessmentGroupMembersQuery(ads)
		upstreamAssessmentGroupQuery = new UpstreamAssessmentGroupQuery(ads)
		upstreamAssignmentQuery = new UpstreamAssignmentQuery(ads)
		jdbc = new NamedParameterJdbcTemplate(ads)
	}

	def getAllAssignments: Seq[UpstreamAssignment] = upstreamAssignmentQuery.execute

	private def yearsToImportArray = yearsToImport.map(_.toString): JList[String]

	// This will be quite a few thousand records, but not more than
	// 20k. Shouldn't cause any memory problems, so no point complicating
	// it by trying to stream or batch the data.
	def getAllAssessmentGroups: Seq[UpstreamAssessmentGroup] = upstreamAssessmentGroupQuery.executeByNamedParam(Map(
		"academic_year_code" -> (yearsToImportArray)))

	def getMembers(group: UpstreamAssessmentGroup): Seq[String] = assessmentGroupMemberQuery.executeByNamedParam(Map(
		"module_code" -> group.moduleCode,
		"academic_year_code" -> group.academicYear.toString,
		"mav_occurrence" -> group.occurrence,
		"assessment_group" -> group.assessmentGroup))

	/**
	 * Iterates through ALL module registration elements in ADS (that's many),
	 * passing each ModuleRegistration item to the given callback for it to process.
	 */
	def allMembers(callback: ModuleRegistration => Unit) {
		val params: JMap[String, Object] = Map(
			"academic_year_code" -> yearsToImportArray)
		jdbc.query(AssignmentImporter.GetAllAssessmentGroupMembers, params, new RowCallbackHandler {
			override def processRow(rs: ResultSet) = {
				callback(ModuleRegistration(
					year = rs.getString("academic_year_code"),
					sprCode = rs.getString("spr_code"),
					occurrence = rs.getString("mav_occurrence"),
					moduleCode = rs.getString("module_code"),
					assessmentGroup = rs.getString("assessment_group")))
			}
		})
	}

	private def yearsToImport = AcademicYear.guessByDate(DateTime.now).yearsSurrounding(0, 1)
}

/**
 * Holds data about an individual student's registration on a single module.
 */
case class ModuleRegistration(year: String, sprCode: String, occurrence: String, moduleCode: String, assessmentGroup: String) {
	def differentGroup(other: ModuleRegistration) =
		year != other.year ||
			occurrence != other.occurrence ||
			moduleCode != other.moduleCode ||
			assessmentGroup != other.assessmentGroup

	/**
	 * Returns an UpstreamAssessmentGroup matching the group attributes.
	 */
	def toUpstreamAssignmentGroup = {
		val g = new UpstreamAssessmentGroup
		g.academicYear = AcademicYear.parse(year)
		g.moduleCode = moduleCode
		g.occurrence = occurrence
		g.assessmentGroup = assessmentGroup
		g
	}
}

object AssignmentImporter {

	val GetAssignmentsQuery = """ 
		select mad.module_code, seq, mad.name, mad.assessment_group, m.department_code
		from module_assessment_details mad
		join module m on (m.module_code = mad.module_code and m.in_use = 'Y')
		where assessment_code = 'A'
		and m.department_code is not null """
	// Department code should be set for any modules since 10/11

	val GetAllAssessmentGroups = """
		select distinct mav.academic_year_code, mav.module_code, mav_occurrence, mad.assessment_group
		from module_availability mav
		join module_assessment_details mad on mad.module_code = mav.module_code
		join module m on (m.module_code = mad.module_code and m.in_use = 'Y')
		where academic_year_code in (:academic_year_code) """

	val GetAssessmentGroupMembers = """
		select spr_code
		from module_registration mr
		where mr.academic_year_code = :academic_year_code
		and mr.module_code = :module_code
		and mr.mav_occurrence = :mav_occurrence
		and mr.assessment_group = :assessment_group
		"""

	val GetAllAssessmentGroupMembers = """
		select 
			academic_year_code,
			spr_code, 
			mav_occurrence,
			module_code,
			assessment_group
		from module_registration  
		where academic_year_code in (:academic_year_code)
		order by academic_year_code, module_code, mav_occurrence, assessment_group
		"""

	class UpstreamAssignmentQuery(ds: DataSource) extends MappingSqlQuery[UpstreamAssignment](ds, GetAssignmentsQuery) {
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) = {
			val a = new UpstreamAssignment
			a.moduleCode = rs.getString("module_code")
			a.sequence = rs.getString("seq")
			a.name = rs.getString("name")
			a.assessmentGroup = rs.getString("assessment_group")
			a.departmentCode = rs.getString("department_code")
			a
		}
	}

	class UpstreamAssessmentGroupQuery(ds: DataSource) extends MappingSqlQueryWithParameters[UpstreamAssessmentGroup](ds, GetAllAssessmentGroups) {
		declareParameter(new SqlParameter("academic_year_code", Types.VARCHAR))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: java.util.Map[_, _]) = {
			val ag = new UpstreamAssessmentGroup()
			ag.moduleCode = rs.getString("module_code")
			ag.academicYear = AcademicYear.parse(rs.getString("academic_year_code"))
			ag.assessmentGroup = rs.getString("assessment_group")
			ag.occurrence = rs.getString("mav_occurrence")
			ag
		}
	}

	class AllAssessmentGroupMembers(ds: DataSource) extends MappingSqlQueryWithParameters[String](ds, GetAllAssessmentGroupMembers) {
		declareParameter(new SqlParameter("academic_year_code", Types.VARCHAR))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: java.util.Map[_, _]) = {
			SprCode.getUniversityId(rs.getString("spr_code"))
		}
	}

	class AssessmentGroupMembersQuery(ds: DataSource) extends MappingSqlQueryWithParameters[String](ds, GetAssessmentGroupMembers) {
		this.declareParameter(new SqlParameter("academic_year_code", Types.VARCHAR))
		this.declareParameter(new SqlParameter("module_code", Types.VARCHAR))
		this.declareParameter(new SqlParameter("mav_occurrence", Types.VARCHAR))
		this.declareParameter(new SqlParameter("assessment_group", Types.VARCHAR))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: java.util.Map[_, _]) = {
			SprCode.getUniversityId(rs.getString("spr_code"))
		}
	}
}