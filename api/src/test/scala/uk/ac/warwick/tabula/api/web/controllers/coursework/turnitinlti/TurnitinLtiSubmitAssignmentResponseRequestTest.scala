package uk.ac.warwick.tabula.api.web.controllers.coursework.turnitinlti

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets
import javax.management.openmbean.SimpleType

import com.fasterxml.jackson.databind.{MapperFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.springframework.http.{HttpHeaders, HttpInputMessage}
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import uk.ac.warwick.tabula.{Mockito, TestBase}

class TurnitinLtiSubmitAssignmentResponseRequestTest extends TestBase with Mockito {

	@Test
	def jsonDeserialize(): Unit = {
		val mapper = new ObjectMapper
		mapper.registerModule(DefaultScalaModule)
		mapper.registerModule(new JodaModule)
		mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)

		val json =
			"""{"resource_tool_placement_url":"https://www.turnitinuk.com/api/lti/1p0/resource_tool_data/7797760?lang=en_us","resource_link_id":"Assignment-c0d5b0a6-8f77-4f01-a4f7-11b03c39e470","assignmentid":7797760}"""

		val request = mapper.readValue(json, classOf[TurnitinLtiSubmitAssignmentResponseRequest])
		request.assignmentid should be ("7797760")
		request.resource_link_id should be ("Assignment-c0d5b0a6-8f77-4f01-a4f7-11b03c39e470")
	}

	@Test
	def jsonDeserializeSpring(): Unit = {
		val converter = new MappingJackson2HttpMessageConverter

		val mapper = new ObjectMapper
		mapper.registerModule(DefaultScalaModule)
		mapper.registerModule(new JodaModule)
		mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
		mapper.configure(MapperFeature.USE_ANNOTATIONS, true)
		converter.setObjectMapper(mapper)

		val json =
			"""{"resource_tool_placement_url":"https://www.turnitinuk.com/api/lti/1p0/resource_tool_data/7797760?lang=en_us","resource_link_id":"Assignment-c0d5b0a6-8f77-4f01-a4f7-11b03c39e470","assignmentid":7797760}"""

		val inputMessage = new HttpInputMessage {
			override def getBody: InputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
			override def getHeaders: HttpHeaders = ???
		}

		val `type` = classOf[TurnitinLtiSubmitAssignmentResponseRequest]
		val contextClass = classOf[TurnitinLtiSubmitAssignmentResponseController]

		val request = converter.read(`type`, contextClass, inputMessage).asInstanceOf[TurnitinLtiSubmitAssignmentResponseRequest]
		request.assignmentid should be ("7797760")
		request.resource_link_id should be ("Assignment-c0d5b0a6-8f77-4f01-a4f7-11b03c39e470")
	}

}
