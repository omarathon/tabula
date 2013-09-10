<#ftl strip_text=true />

<#--
	Wrap the nested content in a modal-header element with a close button.

	If enabled=false, the content is output without wrapping or button.
-->
<#macro header enabled=true>
<#if enabled>
<div class="modal-header">
<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
</#if>
<#nested />
<#if enabled>
</div>
</#if>
</#macro>

<#macro body enabled=true>
<#if enabled><div class="modal-body"></#if>
<#nested />
<#if enabled></div></#if>
</#macro>