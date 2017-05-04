<#escape x as x?html>

<p>
	This will send all submissions in the assignment to Turnitin to be scored for similarity.
	Scoring submissions will take some time so you'll receive an email when the scores are ready to view.
	This upload will only include files that are not already in Turnitin. So, it's okay to send submissions to Turnitin more than once for an assignment.
</p>

<#if errors?? && errors.allErrors?size gt 0>
	<div class="alert alert-error">
		<#list errors.allErrors as error>
			<p><@spring.message code=error.code arguments=error.arguments/></p>
		</#list>
	</div>
<#else>
	<#if incompatibleFiles?size gt 0>
		<div class="alert alert-warning">
			<p>Some attachments won't be sent to Turnitin because they aren't a type that Turnitin is able
				to process (supported types are <strong>MS Word, Acrobat PDF, Postscript, Text, HTML, WordPerfect (WPD) and Rich Text Format</strong>),
				or they are too big to send (maximum Turnitin file size is 20MB)</p>

			<ul class="file-list">
				<#list incompatibleFiles as f>
					<li><i class="icon-file"></i> ${f.name?html}</li>
				</#list>
			</ul>

			<p>You can still continue but these files will be ignored.</p>
		</div>
	</#if>

	<form action="" method="POST">
		<button type="submit" class="btn btn-primary">Send to Turnitin</button>
	</form>
</#if>

</#escape>