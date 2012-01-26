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
	
	@RequestMapping(value=Array("/api/userpicker/query"))
	def query (form:QueryForm, out:Writer) = {
	  val usersByStaff = fetch(form)
	  val (staff, students) = (usersByStaff.getOrElse(true, Nil), usersByStaff.getOrElse(false, Nil))
	  Mav("api/userpicker/results",
	      "staff" -> staff,
	      "students" -> students).noLayout
	  
	  // JSON! Much easier to just generate the HTML though.
//	  def jsonUser(user:User) = Map(
//		  "id" -> user.getUserId,
//		  "name" -> user.getFullName
//		)
//	  json.writeValue(out, Map(
//	      "result" -> Map(
//	          "count" -> usersByStaff.values.foldLeft(0){ _ + _.size },
//		      "users" -> Map(
//		    	   "staff" -> usersByStaff(true).map{jsonUser},
//		    	   "students" -> usersByStaff(false).map{jsonUser}
//		      )
//	      )
//	  ));
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
		  
		def filter:Map[String,String] = 
		  	item("givenName", firstName) ++ item("sn", lastName)
		  	
		  
		private def item(name:String, value:String) = value match {
		  case s:String if s.hasText => Map(name -> (value + "*"))
		  case _ => Map.empty
		}
	}
}
