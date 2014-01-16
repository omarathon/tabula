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
<#if (features.extensions || features.feedbackTemplates)>
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

		<#if !modules?has_content && department.children?has_content>
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

				<#if features.extensions>
				<li>
					<#assign extensions_url><@routes.extensionsettings department /></#assign>
					<@fmt.permission_button permission='Department.ManageExtensionSettings' scope=department action_descr='manage extension settings' href=extensions_url>
						<i class="icon-calendar"></i> Extensions
					</@fmt.permission_button>
			   </li>
			   </#if>

				<#if features.feedbackTemplates>
				<li>
					<#assign feedback_url><@routes.feedbacktemplates department /></#assign>
					<@fmt.permission_button permission='FeedbackTemplate.Create' scope=department action_descr='create feedback template' href=feedback_url>
						<i class="icon-comment"></i> Feedback templates
					</@fmt.permission_button>
			   </li>
			   </#if>

				<#if features.markingWorkflows>
				<li>
					<#assign markingflow_url><@routes.markingworkflowlist department /></#assign>
					<@fmt.permission_button permission='MarkingWorkflow.Read' scope=department action_descr='manage marking workflows' href=markingflow_url>
						<i class="icon-check"></i> Marking workflows
					</@fmt.permission_button>
				</li>
				</#if>

				<li id="feedback-report-button">
					<#assign feedbackrep_url><@routes.feedbackreport department /></#assign>
					<@fmt.permission_button permission='Department.DownloadFeedbackReport' scope=department action_descr='generate a feedback report' href=feedbackrep_url
											data_attr='data-container=body data-toggle=modal data-target=#feedback-report-modal'>
						<i class="icon-book"></i> Feedback report
					</@fmt.permission_button>
					
					<#-- Run this script inline to allow us to build the modal and load the URL before the rest of the page has loaded -->
					<script type="text/javascript">
						(function($) {
							$('#feedback-report-button').on('click', 'a[data-toggle=modal]', function(e){
								e.preventDefault();
								var $this = $(this);
								var target = $this.attr('data-target');
								var url = $this.attr('href');
								$(target).load(url);
							});
						})(jQuery);
					</script>
				</li>

				<li>
					<#assign copy_url><@routes.copyDepartmentsAssignments department /></#assign>
					<@fmt.permission_button
						permission='Assignment.Create'
						scope=department
						action_descr='copy existing assignments'
						href=copy_url>
						<i class="icon-share-alt"></i> Create assignments from previous
					</@fmt.permission_button>
				</li>
				<li>
					<#assign archive_url><@routes.archiveDepartmentsAssignments department /></#assign>
					<@fmt.permission_button
						permission='Assignment.Archive'
						scope=department
						action_descr='archive existing assignments'
						href=archive_url>
						<i class="icon-folder-close"></i> Archive assignments
					</@fmt.permission_button>
				</li>
				<li>
					<#assign settings_url><@routes.displaysettings department />?returnTo=${(info.requestedUri!"")?url}</#assign>
					<@fmt.permission_button
						permission='Department.ManageDisplaySettings'
						scope=department
						action_descr='manage department settings'
						href=settings_url>
						<i class="icon-list-alt"></i> Settings
					</@fmt.permission_button>
				</li>
				</ul>
			</div>
		</#if>

		<#if modules?has_content && !expand_by_default>
		<div class="btn-group dept-show">
			<a class="btn btn-medium use-tooltip" href="#" data-container="body" title="Modules with no assignments are hidden. Click to show all modules." data-title-show="Modules with no assignments are hidden. Click to show all modules." data-title-hide="Modules with no assignments are shown. Click to hide them">
				<i class="icon-eye-open"></i>
				Show
			</a>
		</div>
		</#if>
	</div>
</#if>

<h1 class="with-settings">${department.name}</h1>

<#if !modules?has_content && department.children?has_content>
<p>This department doesn't directly contain any modules. Check subdepartments.</p>
</#if>

<div id="feedback-report-modal" class="modal fade"></div>

<script type="text/javascript">
	<#-- Immediately start waiting for collapsibles to load - don't wait to wire this handler in, because we initialise collapsibles before the DOM has loaded below -->
	jQuery(document.body).on('loaded.collapsible', '.module-info', function() {
		var $module = jQuery(this);
		Courses.zebraStripeAssignments($module);
		$module.find('.use-tooltip').tooltip();
		$module.find('.use-popover').tabulaPopover({
			trigger: 'click',
			container: '#container'
		});
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
