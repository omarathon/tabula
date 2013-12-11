<#if JspTaglibs??>
	<#import "../formatters.ftl" as fmt />
</#if>
<div class="row-fluid">
	<div id="maintenance-message" class="span8 offset2">
		<img src="<@url resource="/static/images/cogs.png" />">
	
		<h1>System under maintenance</h1>
		
		<p>
			We are currently performing essential maintenance to Tabula. At the moment it is only possible to download files. 
			<#if exception.until??>
			Normal access should be
			restored by <strong><@fmt.date date=exception.until capitalise=false at=true relative=true /></strong>.
			<#else>
			Normal access should be restored soon.
			</#if>
		</p>
		
		<#if exception.messageOrEmpty != "">
		<p>
			${exception.messageOrEmpty}
		</p>
		</#if>
		
	</div>
</div>