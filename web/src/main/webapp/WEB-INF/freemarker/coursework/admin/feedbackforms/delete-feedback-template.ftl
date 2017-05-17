<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
<div id="container">
	<#assign submitUrl><@routes.coursework.feedbacktemplatedelete department template /></#assign>
	<@f.form
		method="post"
		class="form-horizontal"
		action=submitUrl
		commandName="deleteFeedbackTemplateCommand">
		<@f.hidden path="id"/>
		<p>Are you sure that you want to delete this feedback template?</p>
	</@f.form>
</div>
</#escape>