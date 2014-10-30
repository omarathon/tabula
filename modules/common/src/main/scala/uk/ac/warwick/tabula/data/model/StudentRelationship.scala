package uk.ac.warwick.tabula.data.model

import java.sql.Types
import org.hibernate.`type`.StandardBasicTypes
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.persistence._
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{ToString, SprCode}
import org.springframework.dao.DataRetrievalFailureException
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import org.hibernate.annotations.BatchSize

@Entity
@AccessType("field")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "agent_type")
/*
 * The relationship is made up of an agent (e.g. tutor), a relationship type and
 * the SPR code of the student - so <some agent> is <some relationship e.g. personal tutor>
 * to <some spr code>
 */
abstract class StudentRelationship extends GeneratedId with Serializable with ToEntityReference {
	type Entity = StudentRelationship

	@transient var profileService = Wire.auto[ProfileService]

	// "agent" is the the actor in the relationship, e.g. tutor
	def agent: String

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "relationship_type")
	var relationshipType: StudentRelationshipType = _

	@ManyToOne(fetch = FetchType.LAZY, cascade = Array(CascadeType.PERSIST))
	@JoinColumn(name = "scjcode")
	var studentCourseDetails: StudentCourseDetails = _

	@Column(name = "uploaded_date")
	var uploadedDate: DateTime = new DateTime

	@Column(name = "start_date")
	var startDate: DateTime = _

	@Column(name = "end_date")
	var endDate: DateTime = _

	// set to true when you don't want permissions to be granted as a result of this relationship - for when a student and the agent fall out
	// at time of writing (2014-10-30) this isn't set anywhere in the app so do this directly in the database
	var isTerminated: Boolean = _
	
	var percentage: JBigDecimal = null

	// assume that all-numeric value is a member (not proven though)
	def isAgentMember: Boolean

	def agentMember: Option[Member]

	def agentName: String
	def agentLastName: String

	def studentMember = Option(studentCourseDetails).map { _.student }
	def studentMember_=(student: StudentMember) { 
		student.mostSignificantCourseDetails.foreach { scd =>
			studentCourseDetails = scd
		}
	}

	def studentId = Option(studentCourseDetails).map { scd => SprCode.getUniversityId(scd.sprCode) }.orNull

	override def toString = super.toString + ToString.forProps("agent" -> agent, "relationshipType" -> relationshipType, "student" -> studentId)

	def toEntityReference = new StudentRelationshipEntityReference().put(this)

	def isCurrent = endDate == null || endDate.isAfterNow
}

@Entity
@DiscriminatorValue("member")
class MemberStudentRelationship extends StudentRelationship {
	def isAgentMember = true
	
	@ManyToOne(fetch = FetchType.LAZY, optional = true, cascade = Array(CascadeType.PERSIST))
	@JoinColumn(name = "agent")
	var _agentMember: Member = _
	def agentMember = Option(_agentMember)
	def agentMember_=(member: Member) { _agentMember = member }
	
	def agentName = agentMember.map( _.fullName.getOrElse("[Unknown]") ).getOrElse(agent)
	def agentLastName = agentMember.map( _.lastName ).getOrElse(agent) // can't reliably extract a last name from agent string
	
	def agent = agentMember.map { _.universityId }.orNull

	override def toEntityReference = new StudentRelationshipEntityReference().put(this)
}

@Entity
@DiscriminatorValue("external")
class ExternalStudentRelationship extends StudentRelationship {
	def isAgentMember = false
	def agentMember = None

	@Column(name = "external_agent_name")
	var _agentName: String = _
	def agent = _agentName
	def agent_=(name: String) { _agentName = name }

	def agentName = agent
	def agentLastName = agent
}

object StudentRelationship {

	def apply(agent: Member, relType: StudentRelationshipType, student: StudentMember) = {
		val rel = new MemberStudentRelationship
		rel.agentMember = agent
		rel.relationshipType = relType
		rel.studentMember = student
		rel
	}

	def apply(agent: Member, relType: StudentRelationshipType, studentCourseDetails: StudentCourseDetails) = {
		val rel = new MemberStudentRelationship
		rel.agentMember = agent
		rel.relationshipType = relType
		rel.studentCourseDetails = studentCourseDetails
		rel
	}

}

object ExternalStudentRelationship {
	def apply(agent: String, relType: StudentRelationshipType, student: StudentMember) = {
		val rel = new ExternalStudentRelationship
		rel.agent = agent
		rel.relationshipType = relType
		rel.studentMember = student
		rel
	}
	
	def apply(agent: String, relType: StudentRelationshipType, studentCourseDetails: StudentCourseDetails) = {
		val rel = new ExternalStudentRelationship
		rel.agent = agent
		rel.relationshipType = relType
		rel.studentCourseDetails = studentCourseDetails
		rel
	}
}