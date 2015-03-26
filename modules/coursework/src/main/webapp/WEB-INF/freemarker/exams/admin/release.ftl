<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#assign formAction><@routes.examReleaseForMarking exam /></#assign>

<@f.form method="post" action="${formAction}" commandName="command">

	<h1>Release ${exam.name} to markers</h1>

	<@form.errors path="" />
	<input type="hidden" name="confirmScreen" value="true" />

	<p>Please confirm you would like to release this exam for marking.</p>

	<div class="submit-buttons">
		<input class="btn btn-warn" type="submit" value="Confirm">
		<a class="btn" href="<@routes.viewExam exam />">Cancel</a>
	</div>

</@f.form>
</#escape>