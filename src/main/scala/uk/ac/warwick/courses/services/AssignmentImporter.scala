package uk.ac.warwick.courses.services

import javax.sql.DataSource
import org.springframework.stereotype.Service
import javax.annotation.Resource
import java.sql.ResultSet
import org.springframework.jdbc.`object`.MappingSqlQuery
import uk.ac.warwick.courses.data.model.UpstreamAssignment
import collection.JavaConversions._
import uk.ac.warwick.courses.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.courses.data.model.UpstreamAssessmentGroup
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters
import org.springframework.jdbc.core.SqlParameter
import java.sql.Types
import uk.ac.warwick.courses.SprCode

@Service
class AssignmentImporter {
	import AssignmentImporter._
	
	@Resource(name="academicDataStore") var ads:DataSource =_
	
	def getAllAssignments:Seq[UpstreamAssignment] = new UpstreamAssignmentQuery(ads).execute
	
	// This will be quite a few thousand records, but not more than
	// 20k. Shouldn't cause any memory problems, so no point complicating
	// it by trying to stream or batch the data.
	def getAllAssessmentGroups:Seq[UpstreamAssessmentGroup] = new UpstreamAssessmentGroupQuery(ads).executeByNamedParam(Map(
		"academic_year_code" -> yearsToImport.map(_.toString)
	))
	
	def getMembers(group:UpstreamAssessmentGroup) = new AssessmentGroupMembersQuery(ads).executeByNamedParam(Map(
		"module_code" -> group.moduleCode,
		"academic_year_code" -> group.academicYear.toString,
		"mav_occurrence" -> group.occurrence,
		"assessment_group" -> group.assessmentGroup
	))
	
	private def yearsToImport = AcademicYear.guessByDate(DateTime.now).yearsSurrounding(2, 2)
}

object AssignmentImporter {
	
	val GetAssignmentsQuery = """ 
		select mad.module_code, seq, mad.name, mad.assessment_group
		from module_assessment_details mad
		join module m on (m.module_code = mad.module_code and m.in_use = 'Y')
		where assessment_code = 'A' """
		
	val GetAllAssessmentGroups = """
		select distinct mav.academic_year_code, mav.module_code, mav_occurrence, mad.assessment_group
		from module_availability mav
		join module_assessment_details mad on mad.module_code = mav.module_code
		join module m on (m.module_code = mad.module_code and m.in_use = 'Y')
		where academic_year_code in :academic_year_code """
		
	val GetAssessmentGroupMembers = """
		select spr_code
		from module_registration mr
		where mr.academic_year_code = :academic_year_code
		and mr.module_code = :module_code
		and mr.mav_occurrence = :mav_occurrence
		and mr.assessment_group = :assessment_group;
		"""
	
	class UpstreamAssignmentQuery(ds:DataSource) extends MappingSqlQuery[UpstreamAssignment](ds, GetAssignmentsQuery) {
		compile()
		override def mapRow(rs:ResultSet, rowNumber:Int) = {
			val a = new UpstreamAssignment
			a.moduleCode = rs.getString("module_code")
			a.sequence = rs.getString("seq")
			a.name = rs.getString("name")
			a.assessmentGroup = rs.getString("assessment_group")
			a
		}
	}
	
	class UpstreamAssessmentGroupQuery(ds:DataSource) extends MappingSqlQueryWithParameters[UpstreamAssessmentGroup](ds, GetAllAssessmentGroups) {
		declareParameter(new SqlParameter("academic_year_code", Types.VARCHAR))
		this.compile()
		override def mapRow(rs:ResultSet, rowNumber:Int, params:Array[java.lang.Object], context:java.util.Map[_,_]) = {
			val ag = new UpstreamAssessmentGroup()
			ag.moduleCode = rs.getString("module_code")
			ag.academicYear = AcademicYear.parse(rs.getString("academic_year_code"))
			ag.assessmentGroup = rs.getString("assessment_group")
			ag.occurrence = rs.getString("mav_occurrence")
			ag
		}
	}
	
	class AssessmentGroupMembersQuery(ds:DataSource) extends MappingSqlQueryWithParameters[String](ds, GetAssessmentGroupMembers) {
		this.declareParameter(new SqlParameter("academic_year_code", Types.VARCHAR))
		this.declareParameter(new SqlParameter("module_code", Types.VARCHAR))
		this.declareParameter(new SqlParameter("mav_occurrence", Types.VARCHAR))
		this.declareParameter(new SqlParameter("assessment_group", Types.VARCHAR))
		this.compile()
		override def mapRow(rs:ResultSet, rowNumber:Int, params:Array[java.lang.Object], context:java.util.Map[_,_]) = {
			SprCode.getUniversityId(rs.getString("spr_code"))
		}
	}
}