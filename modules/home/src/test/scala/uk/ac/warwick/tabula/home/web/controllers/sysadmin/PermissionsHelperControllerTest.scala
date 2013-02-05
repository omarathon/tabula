package uk.ac.warwick.tabula.home.web.controllers.sysadmin
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import uk.ac.warwick.tabula.TestBase
import java.io.File
import java.net.URLStreamHandlerFactory
import org.junit.Before
import org.junit.BeforeClass
import org.scalatest.BeforeAndAfterAll
import java.util.jar.JarFile
import org.springframework.util.FileCopyUtils
import java.io.FileOutputStream

class PermissionsHelperControllerTest extends TestBase {
	
	@Test def sillyUrlType() {
		URL.setURLStreamHandlerFactory(new CatchAllUrlStreamHandlerFactory)
		
		val urlType = new SillyJbossVfsUrlType()
		
		val url = new URL("vfszip:/package/jboss-5.1.0/server/tabula-primary/deploy/home.war/WEB-INF/lib/common-27-SNAPSHOT.jar/")
		
		urlType.matches(url) should be (true)
	}
	
	@Test def argh() {		
		val urlType = new SillyJbossVfsUrlType()
		urlType.createDir(new URL("vfszip:/opt/jboss-5.1.0-GA/server/tabula/deploy/home.war/WEB-INF/lib/common-27-SNAPSHOT.jar/")) should not be (null)
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