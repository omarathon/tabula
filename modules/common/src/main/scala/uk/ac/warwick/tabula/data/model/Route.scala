package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty

import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type

import javax.persistence._
import javax.validation.constraints._

@Entity
@NamedQueries(Array(
	new NamedQuery(name = "route.code", query = "select r from Route r where code = :code"),
	new NamedQuery(name = "route.department", query = "select r from Route r where department = :department")))
class Route extends GeneratedId {

	def this(code: String = null, department: Department = null) {
		this()
		this.code = code
		this.department = department
	}

	@BeanProperty var code: String = _
	@BeanProperty var name: String = _

	@ManyToOne
	@JoinColumn(name = "department_id")
	@BeanProperty var department: Department = _
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.DegreeTypeUserType")
	@BeanProperty var degreeType: DegreeType = _

	@BeanProperty var active: Boolean = _

	override def toString = "Route[" + code + "]"

}