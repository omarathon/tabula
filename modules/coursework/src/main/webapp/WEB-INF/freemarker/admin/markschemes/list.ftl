<h1>Mark schemes</h1>

<p>Mark schemes can be created here and then used by one or more assignments to define how marking is done for that assignment.</p>

<#if !markSchemeInfo?has_content>
<p>
No mark schemes have been created yet. Click <strong>Create</strong> below to make one.
</p>
</#if>

<p><a class="btn" href="<@routes.markschemeadd department=command.department />"><i class="icon-plus"></i> Create</a></p>

<#if markSchemeInfo?has_content>
<table class="mark-schemes table table-bordered table-striped">
<thead>
	<tr>
		<th>Mark scheme name</th>
		<th></th>
	</tr>
</thead>
<tbody>
<#list markSchemeInfo as info>
<#assign markScheme = info.markScheme />
<#assign canDelete = (info.assignmentCount == 0) />
<tr>
	<td>${markScheme.name}</td>
	<td>
		<a class="btn btn-mini" href="<@routes.markschemeedit markScheme />"><i class="icon-edit"></i> Modify</a>
		<a class="btn btn-mini btn-danger<#if !canDelete> use-tooltip disabled</#if>" href="<@routes.markschemedelete markScheme />" data-toggle="modal" data-target="#markscheme-modal"<#if !canDelete> title="You can't delete this mark scheme as it is in use by <@fmt.p info.assignmentCount "assignment" "one" />."</#if>><i class="icon-remove icon-white"></i> Delete</a>
	</td>
</tr>
</#list>
</tbody>
</table>
</#if>

<div id="markscheme-modal" class="modal fade">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		<h3>Delete mark scheme</h3>
	</div>
	<div class="modal-body"></div>
</div>

<script>
jQuery(function($){

$('.mark-schemes').on('click', 'a[data-toggle=modal]', function(e){
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