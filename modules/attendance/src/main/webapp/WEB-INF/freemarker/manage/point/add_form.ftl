<#escape x as x?html>

<#assign heading>
	New monitoring point for ${command.set.route.code?upper_case} ${command.set.route.name}, ${command.set.templateName}
</#assign>

<#if modal??>
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		<h2>${heading}</h2>
	</div>
<#else>
	<h1>${heading}</h1>
</#if>

<#if modal??>
	<div class="modal-body">
</#if>

<#assign action><@url page="/manage/${command.set.route.department.code}/points/add" /></#assign>

<@f.form id="newMonitoringPoint" action="${action}" method="POST" commandName="command" class="form-horizontal">
	<#include "_fields.ftl" />

	<#if modal??>
		<input type="hidden" name="modal" value="true" />
	<#else>
		<div class="submit-buttons">
			<button class="btn btn-primary btn-large">Create</button>
		</div>
	</#if>
</@f.form>

<#if modal??>
	</div>
	<div class="modal-footer">
		<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Creating&hellip;">
	     	Create
	    </button>
	    <button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
	</div>
</#if>


</#escape>