package uk.ac.warwick.tabula.helpers

import java.util.jar.JarFile
import org.reflections.vfs.Vfs
import org.springframework.util.FileCopyUtils
import org.reflections.vfs.SystemDir
import org.reflections.vfs.ZipDir
import org.reflections.ReflectionsException
import java.io.IOException
import java.util.regex.Pattern
import java.net.URL
import com.google.common.base.Predicate
import java.io.File
import java.io.FileOutputStream
import org.reflections.Reflections
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.Permissions
import scala.collection.JavaConverters._

object ReflectionHelper {
	
	Vfs.addDefaultURLTypes(new SillyJbossVfsUrlType)
	lazy val reflections = Reflections.collect()
	
	lazy val allPermissionTargets = {
		def sortFn(clazz1: Class[_ <: PermissionsTarget], clazz2: Class[_ <: PermissionsTarget]) =
			clazz1.getSimpleName < clazz2.getSimpleName
		
		reflections
			.getSubTypesOf(classOf[PermissionsTarget])
			.asScala.toList
			.sortWith(sortFn)
	}
	
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
		
		reflections
			.getSubTypesOf(classOf[Permission])
			.asScala.toList
			.filter {_.getName.substring(Permissions.getClass.getName.length).indexOf('$') != -1}
			.sortWith(sortFn)
			.map { clz => clz.newInstance().asInstanceOf[Permission] }
	}
	
	lazy val groupedPermissions = {
		def groupFn(p: Permission) = {
			val simpleName = p.getClass.getName.substring(Permissions.getClass.getName.length, p.getClass.getName.length -1).replace('$', '.')
			 
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

	private def findFirstMatchOfDeployableExtention(path: String, pos: Int) = {
        val p = Pattern.compile("\\.[ejprw]ar/")
        val m = p.matcher(path)
        if (m.find(pos)) m.end()
        else -1
    }

    private def replaceZipSeparatorStartingFrom(path: String, pos: Int) = {
        val zipFile = path.substring(0, pos - 1)
        var zipPath = path.substring(pos)

        var numSubs = 1
        for (ext <- ReplaceExtension) {
            while (zipPath.contains(ext)) {
        		zipPath = zipPath.replace(ext, ext.substring(0, 4) + "!")
        		numSubs += 1
            }
        }

        var prefix = ""
        for (i <- 1 until numSubs) {
        	prefix += "zip:"
        }

        new URL(prefix + zipFile + "!" + zipPath)
    }

}

class RealFilePredicate extends Predicate[File] {
	def apply(file: File) = file.exists && file.isFile
}