package uk.ac.warwick.tabula.profiles.helpers

import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.profiles.commands.AbstractSearchProfilesCommand
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConversions._
import scala.Some
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.web.Mav


trait SearchJSONHelpers {

	val formMav: Mav

	def toJson(profiles: Seq[Member]) = {
		def memberToJson(member: Member) = Map[String, String](
			"name" -> {member.fullName match {
				case None => "[Unknown user]"
				case Some(name) => name
			}},
			"id" -> member.universityId,
			"userId" -> member.userId,
			"description" -> member.description)

		profiles.map(memberToJson(_))
	}

	def submitJson(cmd: AbstractSearchProfilesCommand, errors: Errors) = {
		if (errors.hasErrors) {
			formMav
		} else {
			val profilesJson: JList[Map[String, Object]] = toJson(cmd.apply())

			Mav(new JSONView(profilesJson))
		}
	}

	def submit(cmd: AbstractSearchProfilesCommand, errors: Errors, path: String) = {
		if (errors.hasErrors) {
			formMav
		} else {
			Mav(path,
				"results" -> cmd.apply())
		}
	}


}
