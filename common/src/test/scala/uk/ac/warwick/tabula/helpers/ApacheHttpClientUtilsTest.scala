package uk.ac.warwick.tabula.helpers

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

import org.apache.http.client.ResponseHandler
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet, HttpRequestBase}
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.BasicHttpResponse
import org.apache.http.{HttpVersion, ProtocolVersion}
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiResponse
import uk.ac.warwick.tabula.{Mockito, TestBase}

class ApacheHttpClientUtilsTest extends TestBase with Mockito {

	val httpClient: CloseableHttpClient = smartMock[CloseableHttpClient]

	@Test
	def xmlResponseHandler(): Unit = {
		val in = new ByteArrayInputStream(
			"""<?xml version="1.0" encoding="UTF-8"?>
				|<response>
				|	 <status>fullsuccess</status>
				|  <submission_data_extract>Some text</submission_data_extract>
				|	 <lis_result_sourcedid>1234</lis_result_sourcedid>
				|	 <message>Your file has been saved successfully.</message>
				|</response>
			""".stripMargin.getBytes(StandardCharsets.UTF_8))

		val out = new ByteArrayOutputStream
		val gzip = new GZIPOutputStream(out)
		FileCopyUtils.copy(in, gzip)

		val response = new BasicCloseableHttpResponse(HttpVersion.HTTP_1_1, 200, "OK")
		response.setEntity(
			EntityBuilder.create()
				.setStream(new ByteArrayInputStream(out.toByteArray))
				.setContentEncoding("gzip")
				.setContentType(ContentType.create("application/xml"))
				.build()
		)

		// Handle each request
		httpClient.execute(any[HttpRequestBase], any[ResponseHandler[Any]]) answers { args =>
			val (_, handler) = args.asInstanceOf[Array[_]].toList match {
				case List(r: HttpRequestBase, h: ResponseHandler[_]) => (r, h)
				case other => throw new IllegalArgumentException(s"Invalid arguments: $other")
			}

			handler.handleResponse(response)
		}

		val handler = ApacheHttpClientUtils.xmlResponseHandler(TurnitinLtiResponse.fromXml)
		val turnitinResponse = httpClient.execute(new HttpGet("https://example.com"), handler)
		turnitinResponse.success should be (true)
		turnitinResponse.statusMessage should be (Some("Your file has been saved successfully."))
	}

	private class BasicCloseableHttpResponse(version: ProtocolVersion, statusCode: Int, statusReason: String) extends BasicHttpResponse(version, statusCode, statusReason) with CloseableHttpResponse {
		override def close(): Unit = {}
	}

}
