package uk.ac.warwick.tabula.web.controllers

import scala.collection.JavaConverters._
import java.io.Writer
import scala.reflect.BeanProperty
import org.codehaus.jackson.map.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import UserPickerController.UserPickerCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.helpers.StringUtils._
import collection.JavaConversions._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._

@Controller
class UserPickerController extends BaseController {
	var json = Wire.auto[ObjectMapper]

	@RequestMapping(value = Array("/api/userpicker/form"))
	def form: Mav = Mav("api/userpicker/form").noLayout()

	@RequestMapping(value = Array("/api/userpicker/query.json"))
	def queryJson(form: UserPickerCommand, out: Writer) = {
		def toJson(user: User) = Map(
			"value" -> user.getUserId,
			"label" -> user.getFullName,
			"type" -> user.getUserType,
			"dept" -> user.getShortDepartment)
		json.writeValue(out, (form.apply() map toJson));
	}

	@RequestMapping(value = Array("/api/userpicker/query"))
	def query(form: UserPickerCommand, out: Writer) = {
		val foundUsers = form.apply()
		val (staff, students) = foundUsers.partition { _.isStaff }
		Mav("api/userpicker/results",
			"staff" -> staff,
			"students" -> students).noLayout()
	}

}

object UserPickerController {
	class UserPickerCommand extends Command[Seq[User]] with ReadOnly with Unaudited {
		PermissionCheck(Permissions.UserPicker)
		
		var userLookup = Wire.auto[UserLookupService]
	
		@BeanProperty var firstName: String = ""
		@BeanProperty var lastName: String = ""
			
		def applyInternal() = {
			var users = userLookup.findUsersWithFilter(filter)
			if (users.size < 10) users ++= userLookup.findUsersWithFilter(filterBackwards)
			
			users.asScala.toSeq
		}

		/**
		 * If one word is given, it's used as surname.
		 * If more words are given, the first two are used
		 * 	as firstname and surname.
		 */
		def setQuery(q: String) {
			firstName = ""
			lastName = ""
			q.split("\\s").toList match {
				case Nil =>
				case surname :: Nil => lastName = surname
				case first :: second :: _ => firstName = first; lastName = second
			}
		}
		def query_=(q: String): Unit = setQuery(q)

		def filter: Map[String, String] = {
			item("givenName", firstName) ++ item("sn", lastName)
		}

		// filter with surname as firstname and viceversa, in case we get no results
		def filterBackwards: Map[String, String] = {
			item("givenName", lastName) ++ item("sn", firstName)
		}

		private def item(name: String, value: String): Map[String, String] = value match {
			case s: String if s.hasText => Map(name -> (value + "*"))
			case _ => Map.empty
		}
	}
}
