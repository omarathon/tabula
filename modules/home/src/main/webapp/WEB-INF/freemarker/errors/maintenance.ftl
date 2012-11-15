<#if JspTaglibs??>
	<#import "../formatters.ftl" as fmt />
</#if>
<div class="alert alert-info">
	<h1>System under maintenance</h1>
	<p>
		At the moment it is only possible to download files. 
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