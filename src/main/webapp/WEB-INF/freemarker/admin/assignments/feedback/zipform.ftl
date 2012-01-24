<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<div id="batch-feedback-form">
<@f.form method="post" enctype="multipart/form-data" action="/admin/module/${module.code}/assignments/${assignment.id}/feedback/batch" commandName="addFeedbackCommand">
<input type="hidden" name="batch" value="true">

<h1>Submit feedback for ${assignment.name}</h1>

<p>The feedback filenames need to contain the student's University ID
	(e.g. <span class="example-filename">0123456 feedback.doc</span>) in order to match each
	file with the correct student. If you are using a Zip file, you could also put each file inside
	a folder whose name includes the University ID
	(e.g. <span class="example-filename">0123456/feedback.doc</span>).
</p>


<table role="presentation" class="narrowed-form">
<tr>
<td id="multifile-column">

<h3>Select file</h3>

<p id="multifile-column-description">
Your browser doesn't seem able to handle uploading multiple files<noscript>
(or it does, but your browser is not running the Javascript needed to support it)
</noscript>.
A recent browser like Google Chrome or Firefox will be able to upload multiple files.
You can still upload a single feedback file here if you want. 
<div id="multifile-column-description-enabled" style="display:none">
This uploader allows you to upload multiple files at once. They
will need to be in the same folder on your computer for you to be
able to select them all.
</div>
</p>

<@form.labelled_row "file.upload" "Files">
<input type="file" name="file.upload" multiple />
</@form.labelled_row>

</td>	
<td id="zip-column">

<h3>&hellip;or use a Zip file <img src="/static/images/icons/package-x-generic.png" alt="" style="margin-bottom: -8px"></h3>

<p>You can pack the files into a Zip file if you prefer, or if your
browser doesn't support uploading multiple files at once.</p>

<@form.labelled_row "archive" "Zip archive">
<input type="file" name="archive" />
</@form.labelled_row>

</td>
</tr>
</table>

<script>
if (Supports.multipleFiles) {
  jQuery('#multifile-column')
  	.find('h3').html('Select files');
  jQuery('#multifile-column-description')
  	.html(jQuery('#multifile-column-description-enabled').html());
}
</script>

<div class="submit-buttons">
<input type="submit" value="Upload">
</div>
</@f.form>
</div>
</#escape>