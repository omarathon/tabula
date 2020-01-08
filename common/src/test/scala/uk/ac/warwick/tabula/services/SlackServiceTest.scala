package uk.ac.warwick.tabula.services

import play.api.libs.json.Json
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.SlackService._

class SlackServiceTest extends TestBase {

  @Test def jsonWrites(): Unit = {
    val message = MessagePayload.blocks(
      fallbackText = "Some text",
      blocks = Seq(
        SectionBlock(
          text = Text.markdown("Some *bold* text")
        ),
        DividerBlock(),
        SectionBlock(
          text = Text.plain("Some *plain* text")
        )
      )
    )

    Json.prettyPrint(Json.toJson(message)) should be (
      """{
        |  "text" : "Some text",
        |  "blocks" : [ {
        |    "text" : {
        |      "type" : "mrkdwn",
        |      "text" : "Some *bold* text",
        |      "verbatim" : false
        |    },
        |    "type" : "section"
        |  }, {
        |    "type" : "divider"
        |  }, {
        |    "text" : {
        |      "type" : "plain_text",
        |      "text" : "Some *plain* text",
        |      "emoji" : false
        |    },
        |    "type" : "section"
        |  } ],
        |  "mrkdwn" : true
        |}""".stripMargin)
  }

}
