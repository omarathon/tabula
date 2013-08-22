<#escape x as x?html>

<#assign heading>
	Delete monitoring point for ${command.set.route.code?upper_case} ${command.set.route.name}, ${command.set.templateName}
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

<#assign action><@url page="/manage/${command.set.route.department.code}/points/delete" /></#assign>

<@f.form id="deleteMonitoringPoint" action="${action}" method="POST" commandName="command" class="form-horizontal">
	<@f.hidden path="set" />
	<@f.hidden path="point" />

	<p>You are deleting the monitoring point: ${command.point.name} (week ${command.point.week}).</p>

	<p>
    	<@form.label checkbox=true>
    		<@f.checkbox path="confirm" /> I confirm that I want to permanently delete this monitoring point.
    	</@form.label>
    	<@form.errors path="confirm"/>
    </p>

	<#if modal??>
		<input type="hidden" name="modal" value="true" />
	<#else>
		<div class="submit-buttons">
			<button class="btn btn-primary btn-large">Delete</button>
		</div>
	</#if>
</@f.form>

<#if modal??>
	</div>
	<div class="modal-footer">
		<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Deleting&hellip;">
	     	Delete
	    </button>
	    <button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
	</div>
</#if>


</#escape>