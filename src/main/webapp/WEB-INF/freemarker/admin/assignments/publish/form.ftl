<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign module=assignment.module />
<#assign department=module.department />

<@f.form method="post" enctype="multipart/form-data" action="/admin/module/${module.code}/assignments/${assignment.id}/feedback/new" commandName="addFeedbackCommand">

<h1>Publish feedback for ${assignment.name}</h1>

<p>This will publish feedback for ${assignment.feedbacks?size} students.</p>

<p>
Publishing feedback will make feedback available for students to download. It can only be
done once for an assignment, and cannot be undone. Be sure that you have received all the
feedback you need before publishing, and then check the box below.
</p>

<@form.labelled_row "confirm" "">
	<@f.checkbox path="confirm" />
	I have read the above and am ready to release feedback to students.
</@form.labelled_row>

<#-- TODO enable/disable submit button as box is checked. -->

<div class="submit-buttons">
<input type="submit" value="Publish">
</div>
</@f.form>

</#escape>