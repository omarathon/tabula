package uk.ac.warwick.tabula.helpers

import java.io.IOException
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.regex.Pattern
import java.util.jar.JarFile
import scala.collection.JavaConverters._
import scala.reflect._
import org.reflections.ReflectionsException
import org.reflections.Reflections
import org.reflections.vfs.Vfs
import org.reflections.vfs.SystemDir
import org.reflections.vfs.ZipDir
import org.springframework.util.FileCopyUtils
import com.google.common.base.Predicate
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

object ReflectionHelper {
	
	Vfs.addDefaultURLTypes(new SillyJbossVfsUrlType)
	lazy val reflections = Reflections.collect()
	
	private def subtypesOf[A : ClassTag] = reflections
			.getSubTypesOf(classTag[A].runtimeClass.asInstanceOf[Class[A]])
			.asScala.toList
	
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
					case clz if clz == classOf[PermissionsSelector[StudentRelationshipType]] => PermissionsSelector.Any[StudentRelationshipType]
					case clz => clz.newInstance().asInstanceOf[Object]
				}
				
				if (params.length == 0) constructor.newInstance().asInstanceOf[Permission]
				else constructor.newInstance(params: _*).asInstanceOf[Permission]
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

/**
 * From http://code.google.com/p/reflections/issues/detail?id=27
 */
class SillyJbossVfsUrlType extends Vfs.UrlType with Logging {
	val ReplaceExtension = Set(".ear/", ".jar/", ".war/", ".sar/", ".har/", ".par/")

	val VfsZipProtocol = "vfszip"
	val VfsFileProtocol = "vfsfile"
		
	def matches(url: URL) = VfsZipProtocol.equals(url.getProtocol) || VfsFileProtocol.equals(url.getProtocol)
	
	def getJar(file: String) = {
		def toJar(pieces: List[String], jarFile: JarFile = null): JarFile = {
			pieces match {
				case Nil => jarFile
				case head :: tail => 
					if (jarFile == null) toJar(tail, new JarFile(head))
					else {
						// Extract the current head to a temporary location
						val tempFile = File.createTempFile("embedded-jar", ".tmp")
						tempFile.deleteOnExit()
						
						// Get the entry in the current jar file, and write it out to the temporary file
						FileCopyUtils.copy(jarFile.getInputStream(jarFile.getEntry(head)), new FileOutputStream(tempFile))
						
						toJar(tail, new JarFile(tempFile))
					}
			} 
		}
		
		toJar(file.split('!').toList.filterNot(_ == "/"))
	}
		
	def createDir(url: URL) = {
		try {
			val adaptedUrl = adaptUrl(url)
			
			val file = adaptedUrl.getFile()
			new ZipDir(getJar(file))
		} catch {
			case e: Exception => try {
				new ZipDir(new JarFile(url.getFile))
			} catch {
				case e: IOException => null
			}
		}
	}
	
	private def createDir(file: File) = try {
		if (file.exists && file.canRead) {
			if (file.isDirectory) new SystemDir(file)
			else new ZipDir(new JarFile(file))
		} else null
	} catch {
		case e: IOException => null
	}
	
	def adaptUrl(url: URL) = 
		if (VfsZipProtocol.equals(url.getProtocol)) replaceZipSeparators(url.getPath, new RealFilePredicate)
		else if (VfsFileProtocol.equals(url.getProtocol)) new URL(url.toString.replace(VfsFileProtocol, "file"))
		else url
		
	def replaceZipSeparators(path: String, predicate: Predicate[File]): URL = {
		var pos = 0
		while (pos != -1) {
			pos = findFirstMatchOfDeployableExtention(path, pos)
			
			if (pos > 0) {
				val file = new File(path.substring(0, pos - 1))
				if (predicate.apply(file))
					return replaceZipSeparatorStartingFrom(path, pos)
			}
		}
		
		throw new ReflectionsException("Unable to identify the real zip file in path '" + path + "'.")
	}
	
	val ExtensionLength = 4

	private def findFirstMatchOfDeployableExtention(path: String, pos: Int) = {
		val p = Pattern.compile("\\.[ejprw]ar/")
		val m = p.matcher(path)
		if (m.find(pos)) m.end()
		else -1
	}

	private def replaceZipSeparatorStartingFrom(path: String, pos: Int) = {
		val zipFile = path.substring(0, pos - 1)
		var zipPath = path.substring(pos)

		var numSubs = 0
		for (ext <- ReplaceExtension) {
			while (zipPath.contains(ext)) {
				zipPath = zipPath.replace(ext, ext.substring(0, ExtensionLength) + "!")
				numSubs += 1
			}
		}

		val prefix = "zip:" * numSubs
		new URL(prefix + zipFile + "!" + zipPath)
	}

}

class RealFilePredicate extends Predicate[File] {
	def apply(file: File) = file.exists && file.isFile
}