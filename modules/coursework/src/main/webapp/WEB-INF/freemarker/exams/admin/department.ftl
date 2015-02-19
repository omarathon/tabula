<#escape x as x?html>
<#import "modules/admin_components.ftl" as components />

<#macro longDateRange start end>
	<#local openTZ><@warwick.formatDate value=start pattern="z" /></#local>
	<#local closeTZ><@warwick.formatDate value=end pattern="z" /></#local>
	<@fmt.date start />
	<#if openTZ != closeTZ>(${openTZ})</#if>
	-<br>
	<@fmt.date end /> (${closeTZ})
</#macro>

<#if department??>

<#assign can_manage_dept=can.do("Department.ManageExtensionSettings", department) />
<#assign expand_by_default = (!can_manage_dept && modules?size lte 5) />

<div class="btn-toolbar dept-toolbar">
	<#if !modules?has_content && department.children?has_content>
		<a class="btn btn-medium dropdown-toggle disabled use-tooltip" title="This department doesn't directly contain any modules. Check subdepartments.">
			<i class="icon-wrench"></i>
			Manage
		</a>
	<#else>
		<div class="btn-group dept-settings">
			<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
				<i class="icon-calendar"></i>
				${academicYear.label}
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu pull-right">
				<#list academicYears as year>
					<li>
						<a href="<@routes.departmentHomeWithYearNoModule department year />">
							<#if year.startYear == academicYear.startYear>
								<strong>${year.label}</strong>
							<#else>
								${year.label}
							</#if>
						</a>
					</li>
				</#list>
			</ul>
		</div>

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
						<i class="icon-list-alt icon-fixed-width"></i> Department settings
					</@fmt.permission_button>
				</li>
				<li>
					<#assign settings_url><@routes.notificationsettings department />?returnTo=${(info.requestedUri!"")?url}</#assign>
					<@fmt.permission_button
						permission='Department.ManageNotificationSettings'
						scope=department
						action_descr='manage department notification settings'
						href=settings_url>
						<i class="icon-envelope icon-fixed-width"></i> Notification settings
					</@fmt.permission_button>
				</li>
			</ul>
		</div>
	</#if>

	<#if modules?has_content && !expand_by_default>
	<div class="btn-group dept-show">
		<a class="btn btn-medium use-tooltip" href="#" data-container="body" title="Modules with no exams are hidden. Click to show all modules." data-title-show="Modules with no exams are hidden. Click to show all modules." data-title-hide="Modules with no exams are shown. Click to hide them">
			<i class="icon-eye-open"></i>
			Show
		</a>
	</div>
	</#if>
</div>


<@fmt.deptheader "" "" department routes "departmenthome" "with-settings" />

<#if !modules?has_content>
	<#if department.children?has_content>
		<p class="alert alert-info"><i class="icon-info-sign"></i> This department doesn't directly contain any modules. Check subdepartments.</p>
	<#else>
		<p class="alert alert-info"><i class="icon-info-sign"></i> This department doesn't contain any modules.</p>
	</#if>
</#if>

<div id="feedback-report-modal" class="modal fade"></div>

<script type="text/javascript">
	<#-- Immediately start waiting for collapsibles to load - don't wait to wire this handler in, because we initialise collapsibles before the DOM has loaded below -->
	jQuery(document.body).on('loaded.collapsible', '.module-info', function() {
		var $module = jQuery(this);
		Exams.zebraStripeExams($module);
		$module.find('.use-tooltip').tooltip();
	});
</script>

<#list modules as module>
	<@components.admin_section module=module expand_by_default=expand_by_default />

	<#if !expand_by_default>
		<#-- If we're not expanding by default, initialise the collapsible immediate - don't wait for DOMReady -->
		<script type="text/javascript">
			GlobalScripts.initCollapsible(jQuery('#module-${module.code}').filter(':not(.empty)'));
		</script>
	</#if>
</#list>

<#else>
	<p>No department.</p>
</#if>
</#escape>
