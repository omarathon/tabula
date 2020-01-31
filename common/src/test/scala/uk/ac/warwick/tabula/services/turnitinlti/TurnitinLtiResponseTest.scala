package uk.ac.warwick.tabula.services.turnitinlti

import uk.ac.warwick.tabula.TestBase

class TurnitinLtiResponseTest extends TestBase {

  @Test def submissionsList(): Unit = {
    val response = TurnitinLtiResponse.fromJson(SubmittedPaperInfo)
    val submissionInfo = response.submissionInfo()
    submissionInfo.overlap should be(Some(67))
    submissionInfo.publication_overlap should be(Some(35))
    submissionInfo.student_overlap should be(Some(67))
    submissionInfo.web_overlap should be(Some(0))
  }

  val SubmittedPaperInfo: String =
    """
	{
		"outcome_originalfile":{
			"roles":[
			"Learner",
			"Instructor"
			],
			"text":null,
			"launch_url":"https://sandbox.turnitin.com/api/lti/1p0/download/orig/200503253?lang=en_us",
			"label":"Download File in Original Format"
		},
		"outcome_grademark":{
			"roles":[
			"Instructor"
			],
			"numeric":{
			"score":null,
			"max":null
		},
			"text":"--",
			"launch_url":"https://sandbox.turnitin.com/api/lti/1p0/dv/grademark/200503253?lang=en_us",
			"label":"Open GradeMark"
		},
		"outcome_pdffile":{
			"roles":[
			"Learner",
			"Instructor"
			],
			"text":null,
			"launch_url":"https://sandbox.turnitin.com/api/lti/1p0/download/pdf/200503253?lang=en_us",
			"label":"Download File in PDF Format"
		},
		"outcome_originalityreport":{
			"text":"67%",
			"roles":[
			"Instructor"
			],
			"numeric":{
			"score":67,
			"max":100
		},
			"label":"Open Originality Report",
			"launch_url":"https://sandbox.turnitin.com/api/lti/1p0/dv/report/200503253?lang=en_us",
			"breakdown":{
			"publications_score":35,
			"internet_score":0,
			"submitted_works_score":67
		}
		},
		"outcome_resubmit":{
			"text":null,
			"roles":[
			"Learner"
			],
			"label":"Resubmit File",
			"launch_url":"https://sandbox.turnitin.com/api/lti/1p0/upload/resubmit/200503253?lang=en_us"
		}
	}

	"""

  @Test def pendingInfo(): Unit = {
    val response = TurnitinLtiResponse.fromJson(PendingPaperInfo)
    val submissionInfo = response.submissionInfo()
    submissionInfo.overlap should be(None)
    submissionInfo.publication_overlap should be(None)
    submissionInfo.student_overlap should be(None)
    submissionInfo.web_overlap should be(None)
  }

  val PendingPaperInfo: String =
    """
      |{
      |  "outcome_grammar": {
      |    "launch_url": "https://www.turnitinuk.com/api/lti/1p0/dv/grammar/119388178?lang=en_us",
      |    "roles": [
      |      "Learner",
      |      "Instructor"
      |    ],
      |    "label": "Open grammar feedback"
      |  },
      |  "outcome_originalityreport": {
      |    "breakdown": {
      |      "internet_score": null,
      |      "publications_score": null,
      |      "submitted_works_score": null
      |    },
      |    "roles": [
      |      "Instructor"
      |    ],
      |    "text": "Pending",
      |    "launch_url": "https://www.turnitinuk.com/api/lti/1p0/dv/report/119388178?lang=en_us",
      |    "numeric": {
      |      "score": null,
      |      "max": 100
      |    },
      |    "label": "Open Originality Report"
      |  },
      |  "meta": {
      |    "date_uploaded": "2020-01-31T09:05:48Z",
      |    "deleted": false
      |  },
      |  "outcome_pdffile": {
      |    "roles": [
      |      "Learner",
      |      "Instructor"
      |    ],
      |    "launch_url": "https://www.turnitinuk.com/api/lti/1p0/download/pdf/119388178?lang=en_us",
      |    "text": null,
      |    "label": "Download File in PDF Format"
      |  },
      |  "outcome_grademark": {
      |    "numeric": {
      |      "max": 100,
      |      "score": null
      |    },
      |    "roles": [
      |      "Instructor"
      |    ],
      |    "launch_url": "https://www.turnitinuk.com/api/lti/1p0/dv/grademark/119388178?lang=en_us",
      |    "text": "--",
      |    "feedback_exists": false,
      |    "marks_exist": false,
      |    "label": "Open GradeMark"
      |  },
      |  "outcome_originalfile": {
      |    "label": "Download File in Original Format",
      |    "roles": [
      |      "Learner",
      |      "Instructor"
      |    ],
      |    "launch_url": "https://www.turnitinuk.com/api/lti/1p0/download/orig/119388178?lang=en_us",
      |    "text": null
      |  },
      |  "outcome_resubmit": {
      |    "label": "Resubmit File",
      |    "roles": [
      |      "Learner"
      |    ],
      |    "text": null,
      |    "launch_url": "https://www.turnitinuk.com/api/lti/1p0/upload/resubmit/119388178?lang=en_us"
      |  },
      |  "outcome_submission_delete": {
      |    "roles": [
      |      "Instructor"
      |    ],
      |    "launch_url": "https://www.turnitinuk.com/api/lti/1p0/submission/delete/119388178?lang=en_us",
      |    "label": "Delete Submission"
      |  }
      |}
      |""".stripMargin
}

