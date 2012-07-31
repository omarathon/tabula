package uk.ac.warwick.courses.data.model

import scala.reflect.BeanProperty
import javax.persistence._
import uk.ac.warwick.userlookup.User
import util.matching.Regex

@Entity
class UpstreamMember {
  import UpstreamMember._

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


	
}

object UpstreamMember {
  val FirstLetterPattern = """(^|\W)(\w)""".r
  val MidwordLetterPattern = """(^|\W)(Mc|Mac)(\w)""".r

  // SITS names are ALLCAPS so try to make them a bit nicer.
  // Upcases any letter not following another letter, to handle puncuated
  private def formatName(name: String) = {
    val lower = name.toLowerCase

    val uppered = FirstLetterPattern.replaceAllIn(lower, { m:Regex.Match =>
      m.group(1) + m.group(2).toUpperCase
    })

    val macUppered = MidwordLetterPattern.replaceAllIn(uppered, {m:Regex.Match =>
      m.group(1) + m.group(2) + m.group(3).toUpperCase
    })

    macUppered
  }
}