package uk.ac.warwick.courses.web.controllers
import java.io.Writer
import scala.reflect.BeanProperty
import org.codehaus.jackson.map.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import UserPickerController.QueryForm
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.courses.services.UserLookupService
import uk.ac.warwick.courses.helpers.StringUtils._
import collection.JavaConversions._
import uk.ac.warwick.userlookup.User

@Controller
class UserPickerController extends BaseController {
	@Autowired var json:ObjectMapper =_
	@Autowired var userLookup:UserLookupService =_
  
	@RequestMapping(value=Array("/api/userpicker/form"))
	def form: Mav = Mav("api/userpicker/form").noLayout
	
	@RequestMapping(value=Array("/api/userpicker/query.json"))
	def queryJson (form:QueryForm, out:Writer) = {
	  def toJson(user:User) = Map(
	    "value" -> user.getUserId,
	    "label" -> user.getFullName,
	    "type" -> user.getUserType,
	    "dept" -> user.getShortDepartment
	  )
	  var users = userLookup.findUsersWithFilter(form.filter)
	  if (users.size < 10) users ++= userLookup.findUsersWithFilter(form.filterBackwards)
	  json.writeValue(out, (users map toJson));
	}

	
	@RequestMapping(value=Array("/api/userpicker/query"))
	def query (form:QueryForm, out:Writer) = {
	  val usersByStaff = fetch(form)
	  val (staff, students) = (usersByStaff.getOrElse(true, Seq.empty), usersByStaff.getOrElse(false, Seq.empty))
	  Mav("api/userpicker/results",
	      "staff" -> staff,
	      "students" -> students).noLayout
	}
	
	/**
	 * Fetches user lookup results, then groups them into a map keyed by
	 * whether they're staff.
	 * 
	 * i.e. result(true) -> collectionOfStaffUsers
	 * 		result(false) -> nonStaffUsers
	 */
	def fetch(form:QueryForm) = {
	  val users = userLookup.findUsersWithFilter(form.filter)
	  users.groupBy{ _.isStaff() }
	}
}

object UserPickerController {
	class QueryForm {
		@BeanProperty var firstName:String = ""
		@BeanProperty var lastName:String = ""
		
		/**
		 * If one word is given, it's used as surname.
		 * If more words are given, the first two are used
		 * 	as firstname and surname.
		 */
		def setQuery(q:String) {
			firstName = ""
			lastName = ""
			q.split("\\s").toList match {
			   case Nil => 
			   case surname::Nil => lastName = surname
			   case first::second::_ => firstName = first; lastName = second
			}
		}
		def query_=(q:String):Unit = setQuery(q)
			
		def filter:Map[String,String] = 
		  	item("givenName", firstName) ++ item("sn", lastName)
		
		// filter with surname as firstname and viceversa, in case we get no results
		def filterBackwards:Map[String,String] = 
			item("givenName", lastName) ++ item("sn", firstName)
		  
		private def item(name:String, value:String) = value match {
		  case s:String if s.hasText => Map(name -> (value + "*"))
		  case _ => Map.empty
		}
	}
}
