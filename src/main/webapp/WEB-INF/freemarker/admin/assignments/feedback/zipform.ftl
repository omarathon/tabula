<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<@f.form method="post" enctype="multipart/form-data" action="/admin/module/${module.code}/assignments/${assignment.id}/feedback/batch" commandName="addFeedbackCommand">

<h1>Submit feedback for ${assignment.name}</h1>

<p>The feedback filenames must either start with the Uni ID of the student they relate to 
	(e.g. <span class="example-filename">0123456 - feedback.doc</span>),
	<em>or</em> they must each be inside a folder named after the Uni ID 
	(e.g. <span class="example-filename">0123456/feedback.doc</span>).</p>

<@form.labelled_row "archive" "Zip archive">
<input type="file" name="archive" />
</@form.labelled_row>

<div class="submit-buttons">
<input type="submit" value="Upload">
</div>
</@f.form>

</#escape>