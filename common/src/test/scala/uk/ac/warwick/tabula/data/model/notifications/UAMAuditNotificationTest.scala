package uk.ac.warwick.tabula.data.model.notifications

import java.io.{ByteArrayOutputStream, OutputStreamWriter}

import org.joda.time.DateTimeUtils
import org.junit.After
import uk.ac.warwick.tabula.data.model.{Department, Notification}
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class UAMAuditNotificationTest extends TestBase {

	@After
	def setTimeBack(): Unit = DateTimeUtils.setCurrentMillisSystem()

	@Test
	def recipientsIsAgent(): Unit = withUser("u1574595", "1574595") {
		Notification.init(new UAMAuditFirstNotification, currentUser.realUser, Seq.empty[Department]).agent.getUserId should be("u1574595")
	}

	@Test
	def titleWithCorrectYear(): Unit = withUser("u1574595", "1574595") {

		DateTimeUtils.setCurrentMillisFixed(1244761200000L) // 2009 June
		Notification.init(new UAMAuditFirstNotification, currentUser.realUser, Seq.empty[Department]).title should be("Tabula Users Audit 2009")

		DateTimeUtils.setCurrentMillisFixed(1608422400000L) // 2020 Dec
		Notification.init(new UAMAuditFirstNotification, currentUser.realUser, Seq.empty[Department]).title should be("Tabula Users Audit 2021")
	}


	@Test
	def correctDeptNameAndPermissionTreeLink(): Unit = withUser("u1574595", "1574595") {

		val depts = Seq(
			Fixtures.department("cs", "computer science"),
			Fixtures.department("csh", "computer science for human"),
			Fixtures.department("csc", "computer science for cat")
		)

		val notification = Notification.init(new UAMAuditFirstNotification, currentUser.realUser, depts)

		notification.content.model("departments") should be(Seq(
			DeptNameWithPermissionTreeUrl("computer science", "/admin/permissions/department/cs/tree"),
			DeptNameWithPermissionTreeUrl("computer science for human", "/admin/permissions/department/csh/tree"),
			DeptNameWithPermissionTreeUrl("computer science for cat", "/admin/permissions/department/csc/tree")
		))
	}

	@Test
	def correctSubDeptNameAndPermissionTreeLink(): Unit = withUser("u1574595", "1574595") {

		object depts {
			val cs: Department = Fixtures.department("cs", "computer science")
			val csh: Department = Fixtures.department("csh", "computer science for human")
			val csc: Department = Fixtures.department("csc", "computer science for cat")
			csh.parent = cs
			csc.parent = cs
			val all: Seq[Department] = Seq(
				cs,
				csh,
				csc,
				Fixtures.department("ma", "maths for ada")
			)
		}

		val notification = Notification.init(new UAMAuditFirstNotification, currentUser.realUser, depts.all)

		notification.content.model("departments") should be(Seq(
			DeptNameWithPermissionTreeUrl("computer science", "/admin/permissions/department/cs/tree"),
			DeptNameWithPermissionTreeUrl("computer science for human", "/admin/permissions/department/csh/tree"),
			DeptNameWithPermissionTreeUrl("computer science for cat", "/admin/permissions/department/csc/tree"),
			DeptNameWithPermissionTreeUrl("maths for ada", "/admin/permissions/department/ma/tree")
		))
	}

	@Test
	def firstEmailRendersCorrectlyWithMultipleDepts(): Unit = withUser("u1574595", "1574595") {

		DateTimeUtils.setCurrentMillisFixed(1608422400000L) // 2020 Dec

		val notification = Notification.init(
			notification = new UAMAuditFirstNotification,
			agent = currentUser.realUser,
			items = Seq(
				Fixtures.department("cs", "computer science"),
				Fixtures.department("csh", "computer science for human")
			)
		)

		val output = new ByteArrayOutputStream
		val writer = new OutputStreamWriter(output)
		val configuration = newFreemarkerConfiguration()
		val template = configuration.getTemplate(notification.templateLocation)
		template.process(notification.content.model, writer)
		writer.flush()
		val renderedResult = output.toString
		renderedResult should be ("We are contacting you because you currently hold the User Access Manager (UAM) role in Tabula. The person assigned to this role should be in a position to oversee the administration of the departments and sub-departments listed in this notification.\n\nTo satisfy data audit requirements, please complete the Tabula User Audit form here:\n\nhttps://warwick.ac.uk/tabulaaudit\n\nIn the form, we ask you to confirm that you can continue to perform this role for computer science, and computer science for human for the academic year 2021/2022 and that you have checked that permission levels in Tabula are accurate.\n\nHere is a list of departments and sub-departments that you should check:\n\ncomputer science - https://tabula.warwick.ac.uk/admin/permissions/department/cs/tree \n\ncomputer science for human - https://tabula.warwick.ac.uk/admin/permissions/department/csh/tree \n\n- Ensure that staff in your department have the appropriate permission levels.\n- Ensure that only those staff necessary have permission to view students’ personal information.\n- In accepting the UAM role, you agree that you are responsible for the accuracy of these permissions - and will monitor permissions periodically. If you are unable to monitor permissions in the future, you should request that the UAM role is assigned to another person within your department.\n\nFor audit purposes, this should be done by 26 September 2021.\n\nIf you should no longer be the UAM or are unable to check the departmental permissions within this timeframe, please let us know as soon as possible. We'll remove the User Access Manager permissions from your account and ask your Head of Department to assign the role to another staff member.\n\nIf you have any questions or wish to discuss this further, please contact the Tabula Team via tabula@warwick.ac.uk.")
	}

	@Test
	def firstEmailRendersCorrectlyWithSingleDept(): Unit = withUser("u1574595", "1574595") {

		DateTimeUtils.setCurrentMillisFixed(1608422400000L) // 2020 Dec

		val notification = Notification.init(
			notification = new UAMAuditFirstNotification,
			agent = currentUser.realUser,
			items = Seq(
				Fixtures.department("cs", "computer science")
			)
		)

		val output = new ByteArrayOutputStream
		val writer = new OutputStreamWriter(output)
		val configuration = newFreemarkerConfiguration()
		val template = configuration.getTemplate(notification.templateLocation)
		template.process(notification.content.model, writer)
		writer.flush()
		val renderedResult = output.toString
		renderedResult should be ("We are contacting you because you currently hold the User Access Manager (UAM) role in Tabula. The person assigned to this role should be in a position to oversee the administration of the departments and sub-departments listed in this notification.\n\nTo satisfy data audit requirements, please complete the Tabula User Audit form here:\n\nhttps://warwick.ac.uk/tabulaaudit\n\nIn the form, we ask you to confirm that you can continue to perform this role for computer science for the academic year 2021/2022 and that you have checked that permission levels in Tabula are accurate.\n\nHere is a list of departments and sub-departments that you should check:\n\ncomputer science - https://tabula.warwick.ac.uk/admin/permissions/department/cs/tree \n\n- Ensure that staff in your department have the appropriate permission levels.\n- Ensure that only those staff necessary have permission to view students’ personal information.\n- In accepting the UAM role, you agree that you are responsible for the accuracy of these permissions - and will monitor permissions periodically. If you are unable to monitor permissions in the future, you should request that the UAM role is assigned to another person within your department.\n\nFor audit purposes, this should be done by 26 September 2021.\n\nIf you should no longer be the UAM or are unable to check the departmental permissions within this timeframe, please let us know as soon as possible. We'll remove the User Access Manager permissions from your account and ask your Head of Department to assign the role to another staff member.\n\nIf you have any questions or wish to discuss this further, please contact the Tabula Team via tabula@warwick.ac.uk.")
	}

	@Test
	def secondEmailHasTheCorrectTemplatePath(): Unit = withUser("u1574595", "1574595") {
		Notification.init(new UAMAuditSecondNotification, currentUser.realUser, Seq.empty[Department]).templateLocation should be("/WEB-INF/freemarker/emails/uam_audit_second_email.ftl")
	}

	@Test
	def secondEmailRendersCorrectly(): Unit = withUser("u1574595", "1574595") {

		DateTimeUtils.setCurrentMillisFixed(1608422400000L) // 2020 Dec

		val notification = Notification.init(
			notification = new UAMAuditSecondNotification,
			agent = currentUser.realUser,
			items = Seq(
				Fixtures.department("cs", "computer science"),
				Fixtures.department("csh", "computer science for human")
			)
		)

		val output = new ByteArrayOutputStream
		val writer = new OutputStreamWriter(output)
		val configuration = newFreemarkerConfiguration()
		val template = configuration.getTemplate(notification.templateLocation)
		template.process(notification.content.model, writer)
		writer.flush()
		val renderedResult = output.toString
		renderedResult should be ("For the Tabula audit of user access permissions, we have not yet received confirmations from all the User Access Managers (UAMs).\n\nIf you have not yet done so, to satisfy data audit requirements, please complete the Tabula User Audit form here:\n\nhttps://warwick.ac.uk/tabulaaudit\n\nHere is a list of departments and sub-departments that you should check:\n\ncomputer science - https://tabula.warwick.ac.uk/admin/permissions/department/cs/tree \n\ncomputer science for human - https://tabula.warwick.ac.uk/admin/permissions/department/csh/tree \n- Ensure that staff in your department have the appropriate permission levels.\n- Ensure that only those staff necessary have permission to view students’ personal information.\n- In accepting the UAM role, you agree that you are responsible for the accuracy of these permissions - and will monitor permissions periodically. If you are unable to monitor permissions in the future, you should request that the UAM role is assigned to another person within your department.\n\nFor audit purposes, this must be done by 26 September 2021.\n\nPlease be aware that, should we not receive a response, due to the audit implications we will need to remove your User Access Manager permissions and ask the Head of Department to select a new User Access Manager.\n\nThanks for your assistance in this matter.")
	}

}