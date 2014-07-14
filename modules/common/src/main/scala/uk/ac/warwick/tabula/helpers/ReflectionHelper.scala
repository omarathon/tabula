package uk.ac.warwick.tabula.helpers

import java.io.IOException
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.regex.Pattern
import java.util.jar.JarFile
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.`type`.filter.AssignableTypeFilter

import scala.collection.JavaConverters._
import scala.reflect._
import org.springframework.util.FileCopyUtils
import com.google.common.base.Predicate
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.{ToEntityReference, Notification, StudentRelationshipType}
import javax.persistence.DiscriminatorValue
import uk.ac.warwick.tabula.roles.{RoleDefinition, SelectorBuiltInRoleDefinition, BuiltInRoleDefinition}
import java.lang.reflect.Modifier

object ReflectionHelper extends Logging {
	
	private def subtypesOf[A : ClassTag] = {
		val scanner = new ClassPathScanningCandidateComponentProvider(false)
		scanner.addIncludeFilter(new AssignableTypeFilter(classTag[A].runtimeClass))
		val components = scanner.findCandidateComponents("uk.ac.warwick.tabula")
		components.asScala.map { _.getBeanClassName }.toSeq.sorted.map(Class.forName).map { _.asInstanceOf[Class[A]] }
	}

	lazy val allNotifications : Map[String, Class[_ <: Notification[ToEntityReference, Unit]]] = {
		val notifications = subtypesOf[Notification[ToEntityReference, Unit]].filter(_.getAnnotation(classOf[DiscriminatorValue]) != null)
		if (notifications.isEmpty) {
			logger.error("Reflections found no Notification classes!")
		}
		notifications.map { n =>
			val discriminator : DiscriminatorValue = n.getAnnotation(classOf[DiscriminatorValue])
			discriminator.value -> n
		}.toMap
	}

	lazy val allPermissionTargets = subtypesOf[PermissionsTarget].sortBy(_.getSimpleName)
	
	lazy val allPermissions = {
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

		subtypesOf[Permission]
			.filter {_.getName.substring(Permissions.getClass.getName.length).contains('$')}
			.sortWith(sortFn)
			.map { clz =>
				val constructor = clz.getConstructors()(0)
				val params = constructor.getParameterTypes().map {
					// FIXME hardcoded to the only type of permissions selector we have atm
					case clz if clz == classOf[PermissionsSelector[StudentRelationshipType]] => PermissionsSelector.Any[StudentRelationshipType]
					case clz => clz.newInstance().asInstanceOf[Object]
				}
				
				if (params.length == 0) constructor.newInstance().asInstanceOf[Permission]
				else constructor.newInstance(params: _*).asInstanceOf[Permission]
			}
	}

	lazy val allBuiltInRoleDefinitions = {
		val selectorDefinitions = subtypesOf[SelectorBuiltInRoleDefinition[_]]

		subtypesOf[BuiltInRoleDefinition]
			.filterNot { clz => Modifier.isAbstract(clz.getModifiers) }
			.filterNot { clz => selectorDefinitions.contains(clz) }
			.sortBy(_.getSimpleName)
			.map { clz =>
				val name =
					if (clz.getSimpleName.endsWith("$")) clz.getSimpleName.substring(0, clz.getSimpleName.length - 1)
					else clz.getSimpleName

				RoleDefinition.of(name)
			}
	}

	lazy val allSelectorBuiltInRoleDefinitionNames = {
		subtypesOf[SelectorBuiltInRoleDefinition[_]]
			.filterNot { clz => Modifier.isAbstract(clz.getModifiers) }
			.sortBy(_.getSimpleName)
			.map { clz =>
				if (clz.getSimpleName.endsWith("$")) clz.getSimpleName.substring(0, clz.getSimpleName.length - 1)
				else clz.getSimpleName
			}
	}
	
	lazy val groupedPermissions = {
		def groupFn(p: Permission) = {
			val simpleName = Permissions.shortName(p.getClass) 
			 
			val parentName = 
				if (simpleName.indexOf('.') == -1) ""
				else simpleName.substring(0, simpleName.lastIndexOf('.'))
			
			parentName
		}
		
		allPermissions
			.groupBy(groupFn)
			.map { case (key, value) => (key, value map { 
				p => (p.getName, p.getName) 
			})}		
	}
	
}