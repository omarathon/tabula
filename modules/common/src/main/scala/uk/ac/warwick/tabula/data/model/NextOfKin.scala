package uk.ac.warwick.tabula.data.model

import scala.reflect.BeanProperty

import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString

@Entity
class NextOfKin extends GeneratedId with ToString {	
	@ManyToOne
	@JoinColumn(name = "member_id")
	@BeanProperty var member: Member = _
	
	@BeanProperty var firstName: String = _
	@BeanProperty var lastName: String = _
	
	@BeanProperty var relationship: String = _
	
	@OneToOne(cascade = Array(ALL))
	@JoinColumn(name="ADDRESS_ID")
	@BeanProperty var address: Address = _
	
	// Daytime phone is the address telephone
	//@BeanProperty var daytimePhone: String = _
	
	@BeanProperty var eveningPhone: String = _
	@BeanProperty var email: String = _
	
	@BeanProperty def fullName = firstName + " " + lastName
		
	def toStringProps = Seq(
		"member" -> member,
		"name" -> (firstName + " " + lastName),
		"relationship" -> relationship)

}