package uk.ac.warwick.tabula.home.web.controllers.sysadmin
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

import uk.ac.warwick.tabula.TestBase

class PermissionsHelperControllerTest extends TestBase {
	
	@Test def sillyUrlType() = {
		val urlType = new SillyJbossVfsUrlType()
		
		val url = new URL("vfszip", "", -1, "/package/jboss-5.1.0/server/tabula-primary/deploy/home.war/WEB-INF/lib/common-27-SNAPSHOT.jar/", new VfsUrlHandler)
		
		urlType.matches(url) should be (true)
	}

}

class VfsUrlHandler extends URLStreamHandler {
	override def openConnection(url: URL): URLConnection = {
		throw new UnsupportedOperationException()
	}
}