package uk.ac.warwick.tabula.web.filters

import java.io.{ByteArrayOutputStream, StringWriter}

import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.log4j.{PatternLayout, Level, WriterAppender}
import org.springframework.mock.web.{MockFilterChain, MockHttpServletResponse, MockHttpServletRequest}
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.TestBase
import org.apache.http.entity.mime

class PostDataLoggingFilterTest extends TestBase {
	val request = new MockHttpServletRequest
	val response = new MockHttpServletResponse
	val chain = new MockFilterChain
	val filter = new PostDataLoggingFilter

	request.setRequestURI("/url.php")

	// Capture POST_LOGGER output into a StringWriter.
	val writer = new StringWriter()
	val appender = new WriterAppender(new PatternLayout("%c - %m%n"), writer)
	filter.postLogger.setLevel(Level.INFO)
	filter.postLogger.addAppender(appender)

	@Test def noParametersAnonymous {
		assert(filter.generateLogLine(request) === "userId= /url.php ")
	}

	@Test def noParametersLoggedIn {
		withUser("ada") {
			assert(filter.generateLogLine(request) === "userId=ada /url.php ")
		}
	}

	@Test def withParametersLoggedIn {
		request.addParameter("sql", "select SYSDATE from hedgefund where snakes='gravy'")
		request.addParameter("multiball", Array("baseball","pinball"))
		withUser("beatrice") {
			assert(filter.generateLogLine(request) === "userId=beatrice /url.php multiball=baseball&multiball=pinball&sql=select SYSDATE from hedgefund where snakes='gravy'")
		}
	}

	@Test def doFilterGet {
		filter.doFilter(request, response, chain)
		assert(writer.toString === "")
	}

	@Test def doFilterPut {
		request.setMethod("PUT")
		filter.doFilter(request, response, chain)
		assert(writer.toString === "")
	}

	@Test def doFilterPost {
		request.setMethod("POST")
		request.addParameter("query", "acomudashun")
		filter.doFilter(request, response, chain)
		assert(writer.toString === "POST_LOGGER - userId= /url.php query=acomudashun\n")
	}

	@Test def doFilterMultipart {
		request.setMethod("POST")
		request.setContentType("multipart/form-data")

		val entity = MultipartEntityBuilder.create
			.addTextBody("confirm","yes")
			.addBinaryBody("submission", Array[Byte](0,0,0,0,0,0,0,0,0,0))
			.build()

		val baos = new ByteArrayOutputStream
		entity.writeTo(baos)
		request.setContent(baos.toByteArray)

		filter.doFilter(request, response, chain)

		// Filter completely skips multipart requests
		assert(writer.toString === "")
	}
}
