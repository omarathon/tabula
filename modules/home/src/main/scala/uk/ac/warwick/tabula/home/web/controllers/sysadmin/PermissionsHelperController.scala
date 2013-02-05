package uk.ac.warwick.tabula.home.web.controllers.sysadmin

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import javax.validation.Valid
import uk.ac.warwick.tabula.home.commands.sysadmin.PermissionsHelperCommand
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.permissions._
import org.reflections.Reflections
import scala.collection.JavaConverters._
import org.reflections.vfs.Vfs
import java.net.URL

@Controller
@RequestMapping(Array("/sysadmin/permissions-helper"))
class PermissionsHelperController extends BaseSysadminController {
	
	validatesSelf[PermissionsHelperCommand]
	
	@RequestMapping(method = Array(GET, HEAD))
	def showForm(form: PermissionsHelperCommand, errors: Errors) =
		Mav("sysadmin/permissions-helper/form").noLayoutIf(ajax)

	@RequestMapping(method = Array(POST))
	def submit(@Valid form: PermissionsHelperCommand, errors: Errors) = {	
		if (errors.hasErrors)
			showForm(form, errors)
		else {
			Mav("sysadmin/permissions-helper/results",
				"results" -> form.apply()
			)
		}
	}
	
	Vfs.addDefaultURLTypes(new SillyJbossVfsUrlType())
	lazy val reflections = Reflections.collect()
	
	@ModelAttribute("allPermissions") def allPermissions = {
		def sortFn(clazz1: Class[_ <: Permission], clazz2: Class[_ <: Permission]) = {			
			// Remove prefix and strip trailing $, then change $ to .
			val shortName1 = Permissions.shortName(clazz1)
			val shortName2 = Permissions.shortName(clazz2)
			
			// Sort by number of dots, then alphabetically
			val dots1: Int = shortName1.split('.').length
			val dots2: Int = shortName2.split('.').length
			
			if (dots1 != dots2) (dots1 < dots2)
			else shortName1 < shortName2
		}
		
		def groupFn(p: Permission) = {
			val simpleName = p.getClass.getSimpleName.substring(Permissions.getClass.getSimpleName.length, p.getClass.getSimpleName.length -1).replace('$', '.')
			val parentName = 
				if (simpleName.indexOf('.') == -1) ""
				else simpleName.substring(0, simpleName.lastIndexOf('.'))
			
			parentName
		}
		
		reflections
			.getSubTypesOf(classOf[Permission])
			.asScala.toList
			.filter {_.getName.substring(Permissions.getClass.getName.length).indexOf('$') != -1}
			.sortWith(sortFn)
			.map { clz => clz.newInstance().asInstanceOf[Permission] }
			.groupBy(groupFn)
			.map { case (key, value) => (key, value map { 
				p => ((if (key == "") "" else key + ".") + p.toString(), p.toString()) 
			})}
	}
	
	@ModelAttribute("allPermissionTargets") def allPermissionTargets = {
		def sortFn(clazz1: Class[_ <: PermissionsTarget], clazz2: Class[_ <: PermissionsTarget]) =
			clazz1.getSimpleName < clazz2.getSimpleName
		
		reflections
			.getSubTypesOf(classOf[PermissionsTarget])
			.asScala.toList
			.sortWith(sortFn)
	}

}

class SillyJbossVfsUrlType extends Vfs.UrlType {
	def matches(url: URL): Boolean = url.getProtocol().equals("vfszip")
    def createDir(url: URL) = Vfs.DefaultUrlTypes.jarUrl.createDir(url)
}