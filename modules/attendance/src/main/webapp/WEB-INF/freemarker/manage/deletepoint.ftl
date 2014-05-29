<#escape x as x?html>

<h1>Delete monitoring point</h1>

<#assign popoverContent><#noescape>
<ul>
	<#list command.schemesToEdit?sort_by("displayName") as scheme>
		<li>${scheme.displayName}</li>
	</#list>
</ul>
</#noescape></#assign>

<p>
	You are deleting this point on
	<a href="#" class="use-popover" data-content="${popoverContent}" data-html="true" data-placement="top">
		<@fmt.p command.schemesToEdit?size "scheme" />:
	</a>
	<p>
		${command.templatePoint.name}
		(<@fmt.interval command.templatePoint.startDate command.templatePoint.endDate />)
	</p>

</p>

<@f.form action="" method="POST" commandName="command" class="form-horizontal">

	<input name="returnTo" value="${returnTo}" type="hidden" />

	<div class="form-actions">
		<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Deleting&hellip;">
			Delete
		</button>
		<a href="${returnTo}" class="btn">Cancel</a>
	</div>
</@f.form>


</#escape>