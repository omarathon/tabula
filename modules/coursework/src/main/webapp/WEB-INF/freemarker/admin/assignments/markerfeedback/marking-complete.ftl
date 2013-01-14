<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#assign formAction><@url page='/admin/module/${module.code}/assignments/${assignment.id}/marker/marking-completed'/></#assign>

<@f.form method="post" action="${formAction}" commandName="markingCompletedCommand">

<h1>Marking completed </h1>

<@form.errors path="" />
<input type="hidden" name="confirmScreen" value="true" />

<@spring.bind path="students">
	<@form.errors path="students" />
	<#assign students=status.actualValue />

	<p>
		<strong><@fmt.p (students?size) "student" /></strong> submissions will be listed as completed. Note that you will not be able
		to make any further changes to the marks or feedback associated with these submissions after this point. If there are
		still changes that have to be made for these submission then click cancel to return to the feedback list.
	</p>
	<#list students as uniId>
		<input type="hidden" name="students" value="${uniId}" />
	</#list>
</@spring.bind>
<p>
	<@form.errors path="confirm" />
	<@form.label checkbox=true><@f.checkbox path="confirm" />
		<#if (students?size > 1)>
			I confirm that I have finished marking these student's submissions.
		<#else>
			I confirm that I have finished marking this student's submission.
		</#if>
	</@form.label>
</p>

<div class="submit-buttons">
	<input class="btn btn-warn" type="submit" value="Confirm">
</div>
</@f.form>
</#escape>