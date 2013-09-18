<#escape x as x?html>

<h1>Monitoring point set templates</h1>

<#if template??>
	<div class="alert alert-success">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Template '${template.templateName}' successfully
		<#if method?? && method == "add">
			created.
		<#elseif method?? && method == "delete">
			deleted.
		<#else>
			updated.
		</#if>
	</div>
<#elseif method?? && method == "reorder">
	<div class="alert alert-success">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Templates successfully reordered.
	</div>
</#if>

<#if templates?size == 0>
	<div class="striped-section">
		<div class="pull-right">
			<a href="<@url page="/sysadmin/pointsettemplates/add" />" class="btn btn-success btn-medium pull-right">
				<i class="icon-plus"></i> Add a new template
			</a>
		</div>
		<h2 class="section-title">Templates</h2>
		<div class="striped-section-contents">
			<div class="item-info">
				<em>No templates exist</em>
			</div>
		</div>
	</div>
<#else>
	<form id="reorderMonitoringPointSet" action="<@url page="/sysadmin/pointsettemplates/reorder"/>" method="POST">
		<p style="color: #818285;"><em>Drag and drop to reorder templates.</em></p>
		<div class="striped-section">
			<div class="pull-right">
				<a href="<@url page="/sysadmin/pointsettemplates/add" />" class="btn btn-success btn-medium pull-right">
					<i class="icon-plus"></i> Add a new template
				</a>
			</div>
			<h2 class="section-title">Templates</h2>
			<div class="striped-section-contents">
				<#assign templateCount = 0 />
					<#list templates as template>
						<div class="item-info row-fluid point" style="cursor: move;">
							<div class="span12">
								<div class="pull-right">
									<a href="<@url page="/sysadmin/pointsettemplates/${template.id}/edit" />" class="btn btn-primary btn-small">Edit</a>
												<a href="<@url page="/sysadmin/pointsettemplates/${template.id}/delete" />" class="btn btn-danger btn-small">Delete</a>
								</div>
								<input type="hidden" name="templates[${templateCount}]" value="${template.id}" />
								<span class="count">${templateCount + 1}</span>. ${template.templateName} (${template.points?size} points)
							</div>
						</div>
						<#assign templateCount = templateCount + 1 />
					</#list>
				</ol>
			</div>
		</div>
		<input type="submit" value="Reorder" class="btn btn-primary"/>
	</form>

	<script>
		jQuery(function($){
			$('.striped-section-contents').sortable({
				axis: 'y',
				cancel: 'a',
				cursor: 'move',
				items: 'div.item-info',
				update: function() {
					$('.striped-section-contents div.item-info').each(function(i){
						$(this).find('span.count').html(i + 1)
							.end().find('input').attr('name', 'templates[' + i + ']');
					});
				}
			});

		});
	</script>
</#if>

</#escape>