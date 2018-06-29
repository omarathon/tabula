package uk.ac.warwick.tabula.data.model.notifications

import java.io.{ByteArrayOutputStream, OutputStream, OutputStreamWriter}

import org.joda.time.{DateTime, DateTimeUtils}
import org.junit.After
import uk.ac.warwick.tabula.data.model.{Department, Notification}
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class UAMAuditFirstNotificationTest extends TestBase {

	@After
	def setTimeBack(): Unit = DateTimeUtils.setCurrentMillisSystem()

	@Test
	def recipientsIsAgent = withUser("u1574595", "1574595") {
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
	def emailRendersCorrectly() = withUser("u1574595", "1574595") {
		val notification = Notification.init(
			notification = new UAMAuditFirstNotification,
			agent = currentUser.realUser,
			items = Seq(
				Fixtures.department("cs", "computer science"),
				Fixtures.department("csh", "computer science for human")
			)
		)

		val output = new ByteArrayOutputStream
		val writter = new OutputStreamWriter(output)
		val configuration = newFreemarkerConfiguration()
		val template = configuration.getTemplate(notification.templateLocation)
		template.process(notification.content.model, writter)
		writter.flush()

		output.toString should be(
			"""
				|We are contacting you because you currently hold the User Access Manager (UAM) role for computer science, computer science for human in Tabula. The person assigned to this role should be in a position to oversee the administration of the departments and sub-departments listed in this notification.
				|
				|To satisfy data audit requirements, please complete the Tabula User Audit form here:
				|
				|https://warwick.ac.uk/tabulaaudit
				|
				|In the form, we ask you to confirm that you can continue to perform this role for computer science, computer science for human for the academic year 2017/2018 and that you have checked that permission levels in Tabula are accurate.
				|
				|Here is a list of departments and sub-departments that you should check:
				|
				|computer science - https://tabula.warwick.ac.uk/admin/permissions/department/cs/tree
				|
				|computer science for human - https://tabula.warwick.ac.uk/admin/permissions/department/csh/tree
				|
				|- Ensure that staff in your department have the appropriate permission levels.
				|- Ensure that only those staff necessary have permission to view students’ personal information.
				|- In accepting the UAM role, you agree that you are responsible for the accuracy of these permissions - and will monitor permissions periodically. If you are unable to monitor permissions in the future, you should request that the UAM role is assigned to another person within your department.
				|
				|For audit purposes, this should be done by 24 September 2018.
				|
				|If you no longer wish to be the UAM or are unable to check the departmental permissions within this timeframe, please let us know as soon as possible. We’ll remove the User Access Manager permissions from your account and ask your Head of Department to assign the role to another staff member.
				|
				|If you have any questions or wish to discuss this further, please contact the Tabula Team via tabula@warwick.ac.uk.
			""".stripMargin)

	}

}