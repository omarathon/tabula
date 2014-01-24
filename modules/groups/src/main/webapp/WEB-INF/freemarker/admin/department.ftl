<#--





This will soon be refactored to use some components from group_components.ftl,
in the same way that tutor_home.ftl and TutorHomeController are currently

If you are doing any work on this, it would be good to do the above first.



-->
<#import "*/group_components.ftl" as components />
<#escape x as x?html>

<#macro longDateRange start end>
	<#local openTZ><@warwick.formatDate value=start pattern="z" /></#local>
	<#local closeTZ><@warwick.formatDate value=end pattern="z" /></#local>
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

	<div class="btn-toolbar dept-toolbar">
		<#if !data.moduleItems?has_content && department.children?has_content>
			<a class="btn btn-medium dropdown-toggle disabled use-tooltip" title="This department doesn't directly contain any modules. Check subdepartments.">
				<i class="icon-wrench"></i>
				Manage
			</a>
		<#else>
			<div class="btn-group dept-settings">
				<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
					<i class="icon-wrench"></i>
					Manage
					<span class="caret"></span>
				</a>
				<ul class="dropdown-menu pull-right">
					<li>
						<#assign settings_url><@routes.displaysettings department />?returnTo=${(info.requestedUri!"")?url}</#assign>
						<@fmt.permission_button
							permission='Department.ManageDisplaySettings'
							scope=department
							action_descr='manage department settings'
							href=settings_url>
							<i class="icon-list-alt icon-fixed-width"></i> Settings
						</@fmt.permission_button>
					</li>

					<#if features.smallGroupTeachingStudentSignUp>
						<li ${data.hasOpenableGroupsets?string(''," class='disabled use-tooltip' title='There are no self-signup groups to open' ")}>
							<#assign open_url><@routes.batchopen department /></#assign>
							<@fmt.permission_button
								permission='SmallGroups.Update'
								scope=department
								action_descr='open small groups'
								href=open_url>
								<i class="icon-unlock-alt icon-fixed-width"></i> Open
							</@fmt.permission_button>
						</li>
						<li ${data.hasCloseableGroupsets?string(''," class='disabled use-tooltip' title='There are no self-signup groups to close' ")}>
							<#assign close_url><@routes.batchclose department /></#assign>
							<@fmt.permission_button
								permission='SmallGroups.Update'
								scope=department
								action_descr='close small groups'
								href=close_url>
								<i class="icon-lock icon-fixed-width"></i> Close
							</@fmt.permission_button>
						</li>
					</#if>
					<li ${data.hasUnreleasedGroupsets?string(''," class='disabled use-tooltip' title='All modules already notified' ")} >
						<#assign notify_url><@routes.batchnotify department /></#assign>
						<@fmt.permission_button
							permission='SmallGroups.Update'
							scope=department
							action_descr='notify students and staff'
							href=notify_url>
							<i class="icon-envelope-alt icon-fixed-width"></i> Notify
						</@fmt.permission_button>
					</li>
					<li<#if !hasGroupAttendance> class="disabled"</#if>>
						<a href="<@routes.departmentAttendance department />"><i class="icon-group icon-fixed-width"></i> Attendance</a>
					</li>
				</ul>
			</div>

			<#if hasModules>
				<div class="btn-group dept-show">
					<a class="btn btn-medium use-tooltip" href="#" data-container="body" title="Modules with no groups are hidden. Click to show all modules." data-title-show="Modules with no groups are hidden. Click to show all modules." data-title-hide="Modules with no groups are shown. Click to hide them">
						<i class="icon-eye-open"></i> Show
					</a>
				</div>
			</#if>
		</#if>
	</div>

	<@fmt.deptheader "" "" department routes "departmenthome" "with-settings" />

	<#if !hasModules>
		<p class="alert alert-info"><i class="icon-info-sign"></i> This department doesn't contain any modules.</p>
	<#elseif !hasGroups>
		<p class="alert alert-info empty-hint"><i class="icon-lightbulb"></i> This department doesn't have any groups set up. Press 'Show' above to show all modules and begin creating groups.</p>
	</#if>

<#-- This is the big list of modules -->
<@components.module_info data=data expand_by_default=(!can_manage_dept && data.moduleItems?size lte 5) />

<div id="modal-container" class="modal fade"></div>
<#else>
	<p>No department.</p>
</#if>

<script>
	jQuery(function($) {
		$('.dept-show').on('click', function() {
			$('.empty-hint').fadeOut('slow');
		});
	});
</script>

</#escape>
