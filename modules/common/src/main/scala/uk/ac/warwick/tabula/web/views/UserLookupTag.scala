package uk.ac.warwick.tabula.web.views

import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateModel
import freemarker.core.Environment
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.UserLookupService
import freemarker.template.utility.DeepUnwrap
import uk.ac.warwick.tabula.JavaImports._
import freemarker.ext.beans.BeansWrapper
import freemarker.template.TemplateException
import collection.JavaConversions._

/**
 * Accepts either id="abc" or ids=["abc","def"] as attributes.
 * Consequently sets a local variable -
 * 	either returned_user = a User
 *  or returned_users = a map of id -> User
 *
 * When users is set, missing_ids will also be a sequence of user IDs
 * that couldn't be found.
 */
class UserLookupTag extends TemplateDirectiveModel {

	@Autowired var userLookup: UserLookupService = _

	override def execute(env: Environment,
		params: JMap[_, _],
		loopVars: Array[TemplateModel],
		body: TemplateDirectiveBody) = {

		val wrapper = env.getObjectWrapper()

		val user = unwrap(params.get("id")).asInstanceOf[String]
		val users = unwrap(params.get("ids")).asInstanceOf[JList[String]]

		if (body == null) {
			throw new TemplateException("UserLookupTag: must have a body", env);
		}
		
		if (user != null) {
			env.getCurrentNamespace().put("returned_user", wrapper.wrap(userLookup.getUserByUserId(user)))
		} else if (users != null) {
			val map = userLookup.getUsersByUserIds(users)
			val missingUserIds = map.values.filterNot(_.isFoundUser()).map(_.getUserId)
			env.getCurrentNamespace().put("returned_users", wrapper.wrap(map))
			env.getCurrentNamespace().put("missing_ids", wrapper.wrap(missingUserIds))
		} else {
			throw new TemplateException("UserLookupTag: Either user or users must be specified", env)
		}

		body.render(env.getOut())

	}

	def unwrap(obj: Any) = {
		if (obj == null) null
		else DeepUnwrap.unwrap(obj.asInstanceOf[TemplateModel])
	}

}