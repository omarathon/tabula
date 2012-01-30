package uk.ac.warwick.courses.services
import uk.ac.warwick.courses.TestBase
import org.junit.Test
import java.util.zip.ZipInputStream
import org.springframework.core.io.ClassPathResource
import org.springframework.util.FileCopyUtils

class ZipServiceTest extends TestBase {
	@Test def readZip {
		val zip = new ZipInputStream(new ClassPathResource("/feedback1.zip").getInputStream)
		val names = Zips.map(zip){ _.getName }.sorted
		names should have ('size(8))
		names should contain("0123456/")
		names should contain("0123456/feedback.doc")
		names should contain("0123456/feedback.mp3")
		names should contain("0123456/feedback.txt")
		names should contain("0123457/")
		names should contain("0123457/crayons.doc")
		names should contain("0123457/feedback.mp3")
		names should contain("marks.csv")
	}
	
	@Test def iterateZip {
		val zip = new ZipInputStream(new ClassPathResource("/feedback1.zip").getInputStream)
		val names = Zips.iterator(zip){ (iterator) =>
			for (i <- iterator) yield i.getName
		}
		names should have ('size(8))
		names should contain("0123456/")
		names should contain("0123456/feedback.doc")
		names should contain("0123456/feedback.mp3")
		names should contain("0123456/feedback.txt")
		names should contain("0123457/")
		names should contain("0123457/crayons.doc")
		names should contain("0123457/feedback.mp3")
		names should contain("marks.csv")
	}
}