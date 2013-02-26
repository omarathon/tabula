<h1>Marking workflows</h1>

<p>Marking workflows can be created here and then used by one or more assignments to define how marking is done for that assignment.</p>

<#if !markingWorkflowInfo?has_content>
<p>
No marking workflows have been created yet. Click <strong>Create</strong> below to make one.
</p>
</#if>

<p><a class="btn" href="<@routes.markingworkflowadd department=command.department />"><i class="icon-plus"></i> Create</a></p>

<#if markingWorkflowInfo?has_content>
<table class="marking-workflows table table-bordered table-striped">
<thead>
	<tr>
		<th>Marking workflow name</th>
		<th></th>
	</tr>
</thead>
<tbody>
<#list markingWorkflowInfo as info>
<#assign markingWorkflow = info.markingWorkflow />
<#assign canDelete = (info.assignmentCount == 0) />
<tr>
	<td>${markingWorkflow.name}</td>
	<td>
		<a class="btn btn-mini" href="<@routes.markingworkflowedit markingWorkflow />"><i class="icon-edit"></i> Modify</a>
		<a class="btn btn-mini btn-danger<#if !canDelete> use-tooltip disabled</#if>" href="<@routes.markingworkflowdelete markingWorkflow />" data-toggle="modal" data-target="#marking-workflow-modal"<#if !canDelete> title="You can't delete this marking workflow as it is in use by <@fmt.p info.assignmentCount "assignment" "assignments" />."</#if>><i class="icon-remove icon-white"></i> Delete</a>
	</td>
</tr>
</#list>
</tbody>
</table>
</#if>

<div id="marking-workflow-modal" class="modal fade">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		<h3>Delete marking workflow</h3>
	</div>
	<div class="modal-body"></div>
</div>

<script>
jQuery(function($){

$('.marking-workflows').on('click', 'a[data-toggle=modal]', function(e){
	var $this = $(this);
	var $modal = $($this.data('target'));
	var $body = $modal.find('.modal-body').empty();
	$body.load($this.attr('href'), function() {
		$body.find('.btn').each(function() {
			if ($(this).text() == 'Cancel') {
				$(this).attr('data-dismiss', 'modal');
			}
		});
	});
});

$("a.disabled").on('click', function(e){e.preventDefault(e); return false;})

});
</script>