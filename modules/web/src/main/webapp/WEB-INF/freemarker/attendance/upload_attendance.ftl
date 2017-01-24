<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

<form action="${uploadUrl}" method="post" enctype="multipart/form-data" style="margin-bottom: 0">

	<#if ajax>
		<@modal.header>
			<h3>Upload attendance from CSV</h3>
		</@modal.header>
	<#else>
		<h1>Upload attendance from CSV</h1>
		<h6><span class="muted">for</span> ${command.templatePoint.name}</h6>
	</#if>

	<@modal.body enabled=ajax>

		<@spring.bind path="command.*">
			<#if status.error>
				<div class="error has-error">
					<@f.errors path="command.*" cssClass="error" />
				</div>
			</#if>
		</@spring.bind>

		<p>
			Each row in the uploaded file should have 2 values: University ID and the attendance.
			Attendance should be specified as one of the following values:
		</p>
		<ul>
			<li>attended</li>
			<li>authorised</li>
			<li>unauthorised</li>
			<li>not-recorded</li>
		</ul>
		<@form.labelled_row "" "File">
			<input type="file" name="file.upload" />
		</@form.labelled_row>
	</@modal.body>

	<@modal.footer enabled=ajax>
		<input class="btn btn-primary spinnable spinner-auto" type="submit" data-loading-text="Uploading&hellip;">
		<button class="btn btn-default" data-dismiss="modal">Cancel</button>
	</@modal.footer>
</form>
</#escape>