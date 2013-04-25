package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.TestBase
import java.io.ByteArrayInputStream
import uk.ac.warwick.util.core.spring.FileUtils

class ZipCreatorTest extends TestBase {
	
	val tmpDir = createTemporaryDirectory
	
	val creator = new ZipCreator() {
		def zipDir = tmpDir
	}
	
	@Test def itWorks {
		val items = Seq(
				ZipFileItem("one.txt", new ByteArrayInputStream("one".getBytes("UTF-8"))),
				ZipFileItem("two.txt", new ByteArrayInputStream("two".getBytes("UTF-8"))),
				ZipFolderItem("folder", Seq(
						ZipFileItem("three.txt", new ByteArrayInputStream("three".getBytes("UTF-8"))),
						ZipFileItem("four.txt", new ByteArrayInputStream("four".getBytes("UTF-8")))
				))
		)
		
		val zip = creator.createUnnamedZip(items)
		zip.exists() should be (true)
		zip.getName() should endWith (".zip")
		
		creator.invalidate(FileUtils.getFileNameWithoutExtension(zip.getName()))
		zip.exists() should be (false)
		
		val name = "myzip/under/a/folder"
		val namedZip = creator.getZip(name, items)
		namedZip.exists() should be (true)
		namedZip.getName() should endWith (".zip")
		
		// getting zip without any items should effectively be a no-op
		creator.getZip(name, Seq()) should be (namedZip)
		
		creator.invalidate(name)
		zip.exists() should be (false)
	}
	
	@Test def trunc {
		creator.trunc("steve", 100) should be ("steve")
		creator.trunc("steve", 5) should be ("steve")
		creator.trunc("steve", 3) should be ("ste")
		
		creator.trunc(".htaccess", 3) should be (".ht")
		
		// We don't include the extension in length calculations to make it easier
		creator.trunc("bill.ted", 3) should be ("bil.ted")
		creator.trunc("bill.ted", 5) should be ("bill.ted")
	}

}