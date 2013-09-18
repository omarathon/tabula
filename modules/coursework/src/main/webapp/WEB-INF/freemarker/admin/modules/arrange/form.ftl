<#assign department=sortModulesCommand.department />
<#escape x as x?html>

<#if saved??>
<div class="alert alert-success">
<a class="close" data-dismiss="alert">&times;</a>
<p>Changes saved.</p>
</div>
</#if>

<h1>Arrange modules for ${department.name}</h1>

<noscript>
<div class="alert">This page requires Javascript.</div>
</noscript>

<p>Drag modules by their <i class='icon-reorder'></i> handles to move them between departments. To select multiple departments,
drag a box from one module name to another. You can also hold the <kbd class="keyboard-control-key">Ctrl</kbd> key to add to a selection.</p>

<@spring.hasBindErrors name="sortModulesCommand">
<#if errors.hasErrors()>
<div class="alert alert-error">
<h3>Some problems need fixing</h3>
<#if errors.hasGlobalErrors()>
	<#list errors.globalErrors as e>
		<div><@spring.message message=e /></div>
	</#list>
<#else>
	<div>See the errors below.</div>
</#if>
</div>
</#if>
</@spring.hasBindErrors>

<@f.form commandName="sortModulesCommand" action="/coursework/admin/department/${department.code}/sort-modules">
<div class="tabula-dnd">	
	<#macro mods department modules>
		<div class="drag-target">
			<h1>${department.name}</h1>
			<ul class="drag-list full-width" data-bindpath="mapping[${department.code}]">
			<#list modules as module>
				<li class="label" title="${module.name}">
					${module.code?upper_case}
					<input type="hidden" name="mapping[${department.code}][${module_index}]" value="${module.id}" />
				</li>
			</#list>
			</ul>
		</div>
	</#macro>
	
	<#list sortModulesCommand.departments as dept> 
		<@mods dept sortModulesCommand.mappingByCode[dept.code]![] />
	</#list>
	
	<input id="sort-modules-submit" class="btn btn-primary" type="submit" value="Save changes" />
</div>
</@f.form>

</#escape> 