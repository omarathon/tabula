<#--





This will soon be refactored to use some components from group_components.ftl,
in the same way that tutor_home.ftl and TutorHomeController are currently

If you are doing any work on this, it would be good to do the above first.



-->
<#import "../group_components.ftl" as components />
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
	<#assign can_manage_dept=data.canManageDepartment />

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
		
		<div class="btn-group dept-settings">
			<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
				<i class="icon-wrench"></i>
				Manage
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu pull-right">
				<li><a href="<@routes.displaysettings department />?returnTo=${(info.requestedUri!"")?url}"><i class="icon-list-alt icon-fixed-width"></i> Display settings</a></li>
                <li ${data.hasUnreleasedGroupsets?string(''," class='disabled use-tooltip' title='All modules already notified' ")} >
                    <a href="<@routes.batchnotify department />"><i class="icon-envelope-alt icon-fixed-width"></i> Notify</a></li>
			</ul>
		</div>

		<div class="btn-group dept-show">
			<a class="btn btn-medium use-tooltip" href="#" data-container="body" title="Modules with no groups are hidden. Click to show all modules." data-title-show="Modules with no groups are hidden. Click to show all modules." data-title-hide="Modules with no groups are shown. Click to hide them">
				<i class="icon-eye-open"></i>
				Show
			</a>
		</div>
	</div>

<#if !data.moduleItems?has_content && department.children?has_content>
<p>This department doesn't directly contain any modules. Check subdepartments.</p>
</#if>

    <#-- This is the big list of modules -->
    <@components.module_info data />

<#--<#list modules as module>
	<#assign can_manage=can.do("Module.ManageSmallGroups", module) />
	<#assign has_groups=(module.groupSets!?size gt 0) />
	<#assign has_archived_groups=false />
	<#list module.groupSets as groupSet>
		<#if groupSet.archived>
			<#assign has_archived_groups=true />
		</#if>
	</#list>

<a id="${module_anchor(module)}"></a>
<div class="module-info striped-section<#if has_groups> collapsible expanded</#if><#if can_manage_dept && !has_groups> empty</#if>" data-name="${module_anchor(module)}">
	<div class="clearfix">
		<div class="btn-group section-manage-button">
		  <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-wrench"></i> Manage <span class="caret"></span></a>
		  <ul class="dropdown-menu pull-right">
		  	<#if can_manage>
					<li><a href="<@routes.moduleperms module />">
						<i class="icon-user icon-fixed-width"></i> Edit module permissions
					</a></li>
				</#if>
				
				<li><a href="<@routes.createset module />"><i class="icon-group icon-fixed-width"></i> Add small groups</a></li>
				
				<#if has_archived_groups>
					<li><a class="show-archived-small-groups" href="#">
							<i class="icon-eye-open icon-fixed-width"></i> Show archived small groups
						</a>
					</li>
				</#if>
				
		  </ul>
		</div>
	
		<h2 class="section-title with-button"><@fmt.module_name module /></h2>
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
						<#if allocated?? && allocated.id == groupSet.id>
							<div class="alert alert-success">
								<a class="close" data-dismiss="alert">&times;</a>
								<p>Changes saved.</p>
							</div>
						</#if>
					
						<#list groupSet.groups as group>
							<div class="group">
								<h4 class="name">
									${group.name!""}
									<small><@fmt.p (group.students.includeUsers?size)!0 "student" "students" /></small>
								</h4>
								
								<ul class="unstyled">
									<#list group.events as event>
										<li>
											&lt;#&ndash; Tutor, weeks, day/time, location &ndash;&gt;

											<@fmt.weekRanges event />,
											${event.day.shortName} <@fmt.time event.startTime /> - <@fmt.time event.endTime />,
											${event.location}
										</li>
									</#list>
								</ul>
							</div>
						</#list>
						
						<#assign unallocatedSize = groupSet.unallocatedStudents?size /> 
						<#if unallocatedSize gt 0>
							<div class="alert">
								<i class="icon-info-sign"></i> <@fmt.p unallocatedSize "student has" "students have" /> not been allocated to a group
							</div>
						</#if>
						
						<#if groupSet.hasAllocated >
                            <#if (!groupSet.releasedToStudents && !groupSet.releasedToTutors)>
							<div class="alert">
								<i class="icon-info-sign"></i> Notifications have not been sent for these groups
							</div>
                           <#elseif (!groupSet.releasedToStudents && groupSet.releasedToTutors)>
                            <div class="alert">
                                 <i class="icon-info-sign"></i> Notifications have not been sent to students for these groups
                             </div>
                            <#elseif (groupSet.releasedToStudents && !groupSet.releasedToTutors)>
                                <div class="alert">
                                    <i class="icon-info-sign"></i> Notifications have not been sent to tutors for these groups
                                </div>
                           </#if>
						</#if>
					</div>

					<div class="span2">
						<div class="btn-toolbar pull-right">
							<div class="btn-group">
							  <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-cog"></i> Actions <span class="caret"></span></a>
							  <ul class="dropdown-menu pull-right">
								<li><a href="<@routes.editset groupSet />"><i class="icon-wrench icon-fixed-width"></i> Edit properties</a></li>
								<li><a href="<@routes.allocateset groupSet />"><i class="icon-random icon-fixed-width"></i> Allocate students</a></li>

                                <li
                                ${groupSet.fullyReleased?string(" class='disabled use-tooltip' title='Already notified' ",'')} >
                                    <a class="notify-group-link" data-toggle="modal" data-target="#modal-container" href="<@routes.releaseset groupSet />">
                                    <i class="icon-envelope-alt icon-fixed-width"></i>
                                    Notify
                                </a></li>

								<li><a class="archive-group-link ajax-popup" data-popup-target=".btn-group" href="<@routes.archiveset groupSet />">
									<i class="icon-folder-close icon-fixed-width"></i>
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
</#list>-->
<div id="modal-container" class="modal fade"></div>
<#else>
<p>No department.</p>
</#if>

</#escape>
