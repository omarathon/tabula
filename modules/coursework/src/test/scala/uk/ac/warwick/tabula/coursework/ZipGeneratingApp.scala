package uk.ac.warwick.tabula.coursework

import java.io.File
import uk.ac.warwick.tabula.coursework.services._
import java.io.ByteArrayInputStream
import java.io.InputStream

object ZipGeneratingApp extends App with ZipCreator {
	def zipDir = new File(System.getProperty("java.io.tmpdir"))
	 
	implicit def stringStream(text:String):InputStream = new ByteArrayInputStream(text.getBytes("UTF-8"))
	
	println("Making zips!")
	println("We'll be using filenames with things like \u03B1\u03B2\u03B3\u03B4\u03B5\u03B6\u03B7\u03B8 in them.")
	
	val zip = getZip("euros-uncompatibles", Seq(
		ZipFileItem("README.txt", "Don't feed them after midnight"),
		ZipFolderItem("Languages", Seq(
			ZipFileItem("Greek - \u03B1\u03B2\u03B3\u03B4\u03B5\u03B6\u03B7\u03B8.txt", "It's all Greek to me."),
			ZipFileItem("English - abcdefghji.txt", "No problems here, hopefully.")
		)),
		ZipFileItem("PRELUDE \u0E01\u0E02\u0E03\u0E04\u0E05\u0E06.txt", "This concludes our presentation.")
	))
}