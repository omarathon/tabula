<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<div id="batch-feedback-form">
<@f.form method="post" enctype="multipart/form-data" action="/admin/module/${module.code}/assignments/${assignment.id}/marks" commandName="addMarksCommand">

<h1>Submit marks for ${assignment.name}</h1>

<p>
The marks spreadsheet that you upload must be an .xlsx file (created in Microsoft Office 2007+). 
The spreadsheet should have three columns in the following order: student ID, mark, grade.
You can use this <a href="<@url resource="/static/files/example.xlsx"/>" >generated spreadsheet</a> as a template.
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
You can still upload a single marks spreadsheet file here if you want. 
<div id="multifile-column-description-enabled" style="display:none">
This uploader allows you to upload multiple marks spreadsheets at once. They
will need to be in the same folder on your computer for you to be
able to select them all.
</div>
</p>

<@form.labelled_row "file.upload" "Files">
<input type="file" name="file.upload" multiple />
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
<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
</div>
</@f.form>
</div>
</#escape>