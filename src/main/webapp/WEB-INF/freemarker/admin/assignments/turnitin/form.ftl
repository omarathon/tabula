<p>
This will send all the submissions in this assignment to Turnitin to be scored for similarity.
It will take a while so you will get an email when it's done. I'll only upload files that aren't
already in Turnitin, so it's okay to do this more than once for an assignment.
</p>

<#if incompatibleFiles?size gt 0>
<div class="alert alert-warning">
<p>Some attachments won't be sent to Turnitin because they aren't a type that Turnitin is able
to process (supported types are <strong>MS Word, Acrobat PDF, Postscript, Text, HTML, WordPerfect (WPD) and Rich Text Format</strong>):</p>

<ul class="file-list">
<#list incompatibleFiles as f>
	<li><i class="icon-file"></i> ${f.name?html}</li>
</#list>
</ul>

<p>You can still continue but these files will be ignored.</p>
</div>
</#if>

<form action="" method="POST">
<button>Send to Turnitin</button>
</form>