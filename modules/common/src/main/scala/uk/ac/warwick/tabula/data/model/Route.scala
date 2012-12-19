package uk.ac.warwick.tabula.data.model

import reflect.BeanProperty
import util.matching.Regex
import collection.JavaConversions._

import javax.validation.constraints._
import javax.persistence._

import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type

import uk.ac.warwick.tabula.actions._

@Entity
@NamedQueries(Array(
	new NamedQuery(name = "route.code", query = "select r from Route r where code = :code"),
	new NamedQuery(name = "route.department", query = "select r from Route r where department = :department")))
class Route extends GeneratedId
	with Viewable with Manageable with Participatable {

	def this(code: String = null, department: Department = null) {
		this()
		this.code = code
		this.department = department
	}

	@BeanProperty var code: String = _
	@BeanProperty var name: String = _

	@OneToOne(cascade = Array(CascadeType.ALL))
	@JoinColumn(name = "participantsgroup_id")
	@BeanProperty var participants: UserGroup = new UserGroup

	// return participants, creating an empty one if missing.
	def ensuredParticipants = {
		ensureParticipantsGroup
		participants
	}

	/** Create an empty participants group if it's null. */
	def ensureParticipantsGroup {
		if (participants == null) participants = new UserGroup
	}

	@ManyToOne
	@JoinColumn(name = "department_id")
	@BeanProperty var department: Department = _
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.DegreeTypeUserType")
	@BeanProperty var degreeType: DegreeType = _

	@BeanProperty var active: Boolean = _

	override def toString = "Route[" + code + "]"

}