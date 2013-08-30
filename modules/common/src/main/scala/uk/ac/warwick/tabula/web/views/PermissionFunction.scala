package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.tabula.JavaImports._
import org.springframework.beans.factory.annotation.Autowired
import freemarker.core.Environment
import freemarker.template.utility.DeepUnwrap
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateModel
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.RequestInfo
import freemarker.template.TemplateMethodModelEx
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.permissions.SelectorPermission
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

/**
 * Freemarker directive to show the contents of the tag
 */
class PermissionFunction extends TemplateMethodModelEx with Logging {

	@Autowired var securityService: SecurityService = _

	override def exec(args: JList[_]): Object = {
		val arguments = args.asScala.toSeq.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }	
		
		val (permission, item) = arguments match {
			case Seq(actionName: String, item: PermissionsTarget) => 
				(Permissions.of(actionName), item)
			case Seq(actionName: String, item: PermissionsTarget, selector: StudentRelationshipType) => // FIXME hard-coded
				(SelectorPermission.of(actionName, selector), item)
			case _ => throw new IllegalArgumentException("Bad args")
		}

		val request = RequestInfo.fromThread.get
		val currentUser = request.user

		securityService.can(currentUser, permission, item): JBoolean
	}

}