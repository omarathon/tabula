package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Cascade
import org.hibernate.annotations.CascadeType._
import org.hibernate.annotations.Type
import scala.reflect.BeanProperty
import javax.persistence._
import uk.ac.warwick.tabula.actions.Viewable
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.userlookup.User
import org.joda.time.DateTime
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.AcademicYear

@Entity
class NextOfKin extends GeneratedId with Viewable with ToString {	
	@ManyToOne
	@JoinColumn(name = "member_id")
	@BeanProperty var member: Member = _
	
	@BeanProperty var firstName: String = _
	@BeanProperty var lastName: String = _
	
	@BeanProperty var relationship: String = _
	
	
	@OneToOne
	@Cascade(Array(SAVE_UPDATE, DETACH))
	@JoinColumn(name="ADDRESS_ID")
	@BeanProperty var address: Address = _
	
	// Daytime phone is the address telephone
	//@BeanProperty var daytimePhone: String = _
	
	@BeanProperty var eveningPhone: String = _
	@BeanProperty var email: String = _
		
	def toStringProps = Seq(
		"member" -> member,
		"name" -> (firstName + " " + lastName),
		"relationship" -> relationship)

}