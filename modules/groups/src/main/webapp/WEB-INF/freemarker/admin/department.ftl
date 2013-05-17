<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>

<#macro longDateRange start end>
	<#assign openTZ><@warwick.formatDate value=start pattern="z" /></#assign>
	<#assign closeTZ><@warwick.formatDate value=end pattern="z" /></#assign>
	<@fmt.date start /> 
	<#if openTZ != closeTZ>(${openTZ})</#if>
	-<br>
	<@fmt.date end /> (${closeTZ})
</#macro>

<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>

<#if department??>
	<#assign can_manage_dept=can.do("Module.ManageSmallGroups", department) />

	<h1 class="with-settings">
		${department.name}
	</h1>
	
	<div class="btn-toolbar dept-toolbar">
	
		<#if department.parent??>
			<a class="btn btn-medium use-tooltip" href="<@routes.departmenthome department.parent />" data-container="body" title="${department.parent.name}">
				Parent department
			</a>
		</#if>

		<#if department.children?has_content>
			<div class="btn-group">
				<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
					Subdepartments
					<span class="caret"></span>
				</a>
				<ul class="dropdown-menu pull-right">
					<#list department.children as child>
						<li><a href="<@routes.departmenthome child />">${child.name}</a></li>
					</#list>
				</ul>
			</div>
		</#if>

		<div class="btn-group dept-show">
			<a class="btn btn-medium use-tooltip" href="#" data-container="body" title="Modules with no groups are hidden. Click to show all modules." data-title-show="Modules with no groups are hidden. Click to show all modules." data-title-hide="Modules with no groups are shown. Click to hide them">
				<i class="icon-eye-open"></i>
				Show
			</a>
		</div>
	</div>

<#if !modules?has_content && department.children?has_content>
<p>This department doesn't directly contain any modules. Check subdepartments.</p>
</#if>

<#list modules as module>
	<#assign can_manage=can.do("Module.ManageSmallGroups", module) />
	<#assign has_groups=(module.groupSets!?size gt 0) />
	<#assign has_archived_groups=false />
	<#list module.groupSets as groupSet>
		<#if groupSet.archived>
			<#assign has_archived_groups=true />
		</#if>
	</#list>

<a id="${module_anchor(module)}"></a>
<div class="striped-section<#if has_groups> collapsible expanded</#if><#if can_manage_dept && !has_groups> empty</#if>" data-name="${module_anchor(module)}">
	<div class="clearfix">
		<h2 class="section-title with-button"><@fmt.module_name module /></h2>
		<div class="btn-group section-manage-button">
		  <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-wrench"></i> Manage <span class="caret"></span></a>
		  <ul class="dropdown-menu pull-right">
		  	<#if can_manage>	
					<#assign module_managers_count = ((module.managers.includeUsers)![])?size />
					<#-- TODO this needs breaking out of coursework -->
					<li><a href="<@url page="/admin/module/${module.code}/permissions" context="/coursework" />">
						<i class="icon-user"></i> Edit module permissions
					</a></li>
				</#if>
				
				<li><a href="<@url page="/admin/module/${module.code}/groups/new" />"><i class="icon-folder-close"></i> Add small groups</a></li>
				
				<#if has_archived_groups>
					<li><a class="show-archived-small-groups" href="#">
							<i class="icon-eye-open"></i> Show archived small groups
						</a>
					</li>
				</#if>
				
		  </ul>
		</div>
	</div>
	
	
	<#if has_groups>
	<div class="striped-section-contents">
		<#list module.groupSets as groupSet>
			<#if !groupSet.deleted>
				<div class="item-info row-fluid<#if groupSet.archived> archived</#if>">
					<div class="span3">
						<h3 class="name">
							<small>
								${groupSet.name}
								<#if groupSet.archived>
									(Archived)
								</#if>
							</small>
						</h3>
						
						<span class="format">
							${groupSet.format.description}
						</span>
					</div>
					
					<div class="span7">
						<#list groupSet.groups?chunk(2) as row>
							<div class="row groups">
								<#list row as group>
									<div class="group span6">
										<h4 class="name">
											<small>
												${group.name}
											</small>
										</h4>
										
										<#list group.events as event>
											${event.day.name}
										</#list>
									</div>
								</#list>
							</div>
						</#list>
					</div>
					
					<div class="span2">
						<div class="btn-toolbar pull-right">
							<div class="btn-group">
							  <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-cog"></i> Actions <span class="caret"></span></a>
							  <ul class="dropdown-menu pull-right">
								<li><a href="<@url page="/admin/module/${module.code}/groups/${groupSet.id}/edit" />"><i class="icon-wrench"></i> Edit properties</a></li>
								<li><a class="archive-group-link ajax-popup" data-popup-target=".btn-group" href="<@url page="/admin/module/${module.code}/groups/${groupSet.id}/archive" />">
									<i class="icon-folder-close"></i>
									<#if groupSet.archived>
										Unarchive groups
									<#else> 
										Archive groups
									</#if>
								</a></li>
							  </ul>
							</div>
						</div>
					</div>
				</div>
			</#if>
		</#list>
	</div>
	</#if>
	
</div>
</#list>
<#else>
<p>No department.</p>
</#if>

</#escape>
