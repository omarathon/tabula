package uk.ac.warwick.courses.data.model

import scala.reflect.BeanProperty
import javax.persistence._
import uk.ac.warwick.userlookup.User

@Entity
class UpstreamMember {

	@Id @BeanProperty var universityId: String =_
	@BeanProperty @Column(nullable=false) var userId: String =_
	@BeanProperty var firstName: String =_
	@BeanProperty var lastName: String =_
	@BeanProperty var email: String =_
	
	def asSsoUser = {
		val u = new User
		u.setUserId(userId)
		u.setWarwickId(universityId)
		u.setFirstName(formatName(firstName))
		u.setLastName(formatName(lastName))
		u.setFullName(u.getFirstName+" "+u.getLastName)
		u.setEmail(email)
		u.setFoundUser(true)
		u
	}
	
	// SITS names are ALLCAPS so try to make them a bit nicer:
	private def formatName(name: String) = name.toLowerCase.capitalize
	
}