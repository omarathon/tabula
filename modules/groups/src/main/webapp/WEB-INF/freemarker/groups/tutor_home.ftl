<#import "../group_components.ftl" as components />
<#escape x as x?html>

<h1>My small groups</h1>

<#-- Hide while it doesn't actually do anything
<div class="input-prepend">
	<span class="add-on"><i class="icon-search"></i></button></span>
	<input class="span4" type="text" placeholder="Search for a module, student, group">
</div>
-->

<@components.module_info data />

</#escape>