package uk.ac.warwick.tabula.web.controllers

import FlexiPickerController.FlexiPickerCommand
import java.io.{UnsupportedEncodingException, Writer}
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import uk.ac.warwick.userlookup.webgroups.{GroupNotFoundException, GroupServiceException}
import java.net.URLEncoder
import uk.ac.warwick.util.web.view.json.{JSONPRequestValidator, JSONView}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.helpers.StringUtils._
import collection.JavaConversions._
import uk.ac.warwick.userlookup.{Group, User}
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._


@Controller
class FlexiPickerController extends BaseController {
	var json = Wire.auto[ObjectMapper]

	@RequestMapping(value = Array("/api/flexipicker/form"))
	def form: Mav = Mav("api/flexipicker/form").noLayout()


	@RequestMapping(value = Array("/api/flexipicker/query.json"))
	def queryJson(form: FlexiPickerCommand, out: Writer) = {
		var results: List[Map[String, String]] = null

		if (form.hasQuery && form.query.trim.length > 2) {
			results = form.apply
		}

		json.writeValue(out, Map("data" -> Map("results" -> results)))
	}

	/*
	@RequestMapping(value = Array("/api/flexipicker/query"))
	def query(form: FlexiPickerCommand, out: Writer) = {
		val foundUsers = form.apply()
		//val (staff, students) = foundUsers.partition { _.isStaff }
		Mav("api/userpicker/results",
			"staff" -> staff,
			"students" -> students).noLayout()
	}

    */
}



object FlexiPickerController {
	class FlexiPickerCommand extends Command[List[Map[String, String]]] with ReadOnly with Unaudited {
		PermissionCheck(Permissions.UserPicker)

		val ENOUGH_RESULTS = 10
		var includeUsers = true
		var includeGroups = false
		var includeEmail = false

		val EMAIL_PATTERN = "^\\s*(?:([^<@]+?)\\s+)<?([^\\s]+?@[^\\s]+\\.[^\\s]+?)>?\\s*$".r

		var exact = false           // really?
		var universityId = false    // when returning users, use university ID as value?

		var userLookup = Wire.auto[UserLookupService]

		var firstName: String = ""
		var lastName: String = ""

		var query: String = ""

		def applyInternal() = {
			var results = List[Map[String, String]]()

			if(includeEmail)  results = results ++ parseEmail       // add results of email search to our list
			if(includeUsers)  results = results ++ searchUsers      // add results of user search to our list
			if(includeGroups) results = results ++ searchGroups     // add results of user search to our list

			results
		}

		private def searchUsers : List[Map[String, String]] = {
			var users = List[User]()

			if(hasQuery) {
				val terms: Array[String] = query.trim.replace(",", " ").replaceAll("\\s+", " ").split(" ")
				users = doSearchUsers(terms)
			}

			users.map(createItemFor(_))
		}

		private def createItemFor(user: User): Map[String, String] = {
			Map("type" -> "user", "name" -> user.getFullName, "department" -> user.getDepartment, "value" -> getValue(user))
		}


		def hasQuery: Boolean = {
			query.hasText
		}

		/** Return appropriate value for a user, either warwick ID or usercode. */
		private def getValue(user: User): String = {
			if (universityId) {
				user.getWarwickId
			}
			else {
				user.getUserId
			}
		}


		/**
		 * Search users using the given terms.
		 *
		 * Algorithm cribbed from Files.Warwick's UserDAOImpl.
		 *
		 * @param terms Search terms extracted from query.
		 * @param users Output list.
		 */
		private def doSearchUsers(terms: Array[String]): List[User] = {
			var users: List[User] = List[User]()
			if (exact) {
				if (terms.length == 1) {
					val user = userLookup.getUserByUserId(terms(0))
					if (user.isFoundUser) {
						users :+ user
					}
				}
			}
			else if (terms.length == 1) {
				val user = userLookup.getUserByUserId(terms(0))
				if (user.isFoundUser) {
					users = users ++ List(user)
				}
				else {
					users  = users ++  userLookup.findUsersWithFilter(item("sn" , terms(0)))
					if (users.size < ENOUGH_RESULTS) {
						users  = users ++ userLookup.findUsersWithFilter(item("givenName", terms(0)))
					}
					if (users.size < ENOUGH_RESULTS) {
						users  = users ++  userLookup.findUsersWithFilter(item("cn", terms(0)))
					}
				}
			}
			else if (terms.length >= 2) {
				users  = users ++  userLookup.findUsersWithFilter(item("givenName", terms(0)) ++ item("sn", terms(1)))
				if (users.size < ENOUGH_RESULTS) {
					users  = users ++  userLookup.findUsersWithFilter(item("sn", terms(0)) ++ item("givenName", terms(1)))
				}
			}

			users.sortBy(_.getFullName)
			users
		}

		private def searchGroups: List[Map[String, String]] = {
			var list: List[Map[String, String]] = List[Map[String, String]]()
			if (hasQuery) {
				try {
					list = doGroupSearch()
				}
				catch {
					case e: GroupServiceException => {
						logger.error("Failed to look up group names", e)
					}
					case e: UnsupportedEncodingException => {
						throw new IllegalStateException("UTF-8 Does Not Exist??")
					}
					case e: GroupNotFoundException => {
						logger.debug("Group didn't exist")
					}
				}
			}
			list
		}

		private def doGroupSearch(): List[Map[String, String]] =  {
			var outList: List[Map[String, String]] = List[Map[String, String]]()
			if (exact) {
				if (!query.trim.contains(" ")) {
					val group: Group = userLookup.getGroupService.getGroupByName(query.trim)
					outList  = outList ++ List(createItemFor(group))
				}
			}
			else {
				val groupQuery = URLEncoder.encode(query.trim, "UTF-8")
				val groups: List[Group] = userLookup.getGroupService.getGroupsForQuery(groupQuery).toList

				for (group <- groups) {
					outList = outList ++ List(createItemFor(group))
				}
			}

			outList
		}

		private def createItemFor(group: Group): Map[String, String] = {
			Map("type" -> "group", "title" -> group.getTitle, "groupType" -> group.getType, "value" -> group.getName)
		}

		private def parseEmail: List[Map[String, String]] = {
			var list: List[Map[String, String]] = List[Map[String, String]]()
			if (hasQuery) {
				val matched = EMAIL_PATTERN.findAllIn(query.trim).matchData.toList

				if (matched.length>0) {
					val firstMatch = matched(0)
					val builder : Map[String, String] = Map("type" -> "email", "address" -> firstMatch.group(2))

					if (firstMatch.group(1) != null) {
						builder.put("name", firstMatch.group(1))
						builder.put("value", firstMatch.group(1) + " <" + firstMatch.group(2) + ">")
					}
					else {
						builder.put("value", firstMatch.group(2))
					}
					list = list ++ List(builder)
				}
			}
			list
		}





		/**
		 * If one word is given, it's used as surname.
		 * If more words are given, the first two are used
		 * 	as firstname and surname.
		 */
		/*
		def setQuery(q: String) {
			firstName = ""
			lastName = ""
			q.split("\\s").toList match {
				case Nil =>
				case surname :: Nil => lastName = surname
				case first :: second :: _ => firstName = first; lastName = second
			}
		} */

		//def query = if (firstName == "") lastName else firstName + " " + lastName
		//def query_=(q: String) = setQuery(q)

		/*

		def filter: Map[String, String] = {
			item("givenName", firstName) ++ item("sn", lastName)
		}

		// filter with surname as firstname and viceversa, in case we get no results
		def filterBackwards: Map[String, String] = {
			item("givenName", lastName) ++ item("sn", firstName)
		}
		*/

		private def item(name: String, value: String): Map[String, String] = value match {
			case s: String if s.hasText => Map(name -> (value + "*"))
			case _ => Map.empty
		}
	}
}