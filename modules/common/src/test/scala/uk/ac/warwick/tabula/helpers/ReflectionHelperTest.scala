package uk.ac.warwick.tabula.helpers

import java.io.FileReader
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory
import org.reflections.Reflections
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permissions
import java.io.InputStreamReader

class ReflectionHelperTest extends TestBase {
	
	new Reflections("uk.ac.warwick.tabula").save(getClass.getResource("/").getFile() + "META-INF/reflections/all-reflections.xml")
	
	@Test def allPermissionsTargets = {
		ReflectionHelper.allPermissionTargets.contains(classOf[Department]) should be (true) 
	}
	
	@Test def allPermissions = {
		ReflectionHelper.allPermissions.contains(Permissions.Module.ManageAssignments) should be (true) 
	}
	
	@Test def groupedPermissions = {
		ReflectionHelper.groupedPermissions("Module").contains(("Module.Create", "Module.Create")) should be (true) 
	}
	
	@Test def sillyJbossVfsUrlType {
		URL.setURLStreamHandlerFactory(new CatchAllUrlStreamHandlerFactory)
		val urlType = new SillyJbossVfsUrlType
		
		val warFile = getClass().getResource("/silly-jboss-vfs.war").getFile()
		val pathInWarFile = "WEB-INF/lib/embedded-jar.jar"
		
		val zipUrl = new URL(null, "vfszip:" + warFile + "/" + pathInWarFile + "/")
		
		urlType.matches(zipUrl) should be (true)
		urlType.adaptUrl(zipUrl).toExternalForm should be ("zip:" + warFile + "!" + pathInWarFile + "!")
		
		val dir = urlType.createDir(zipUrl)
		val itr = dir.getFiles().iterator()
		val (manifest, txtFile) = (itr.next(), itr.next())
		itr.hasNext() should be (false)
		
		FileCopyUtils.copyToString(new InputStreamReader(txtFile.openInputStream())).trim should be ("yes!")
	}

}

class CatchAllUrlStreamHandlerFactory extends URLStreamHandlerFactory {
	override def createURLStreamHandler(protocol: String) = 
		if ("vfszip".equals(protocol) || "zip".equals(protocol)) new CatchAllUrlHandler
		else null
}

class CatchAllUrlHandler extends URLStreamHandler {
	override def openConnection(url: URL): URLConnection = {
		throw new UnsupportedOperationException()
	}
}