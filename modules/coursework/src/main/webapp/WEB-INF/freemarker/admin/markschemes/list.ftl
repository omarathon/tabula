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
<tr>
	<th>Mark scheme name</th>
	<th></th>
</tr>
<#list markSchemeInfo as info>
<#assign markScheme = info.markScheme />
<#assign canDelete = (info.assignmentCount == 0) />
<tr>
	<td>${markScheme.name}</td>
	<td>
		<a class="btn btn-mini" href="<@routes.markschemeedit markScheme />"><i class="icon-edit"></i> Modify</a>
		<a class="btn btn-mini btn-danger use-tooltip <#if !canDelete>disabled</#if>" 
			href="<@routes.markschemedelete markScheme />" 
			data-toggle="modal" data-target="#markscheme-modal" 
			<#if !canDelete>title="You can't delete this mark scheme as it is in use by <@fmt.p info.assignmentCount "assignment" "one" />."</#if>
			<i class="icon-remove icon-white"></i> Delete 
		</a>
	</td>
</tr>
</#list>
</table>
</#if>

<div id="markscheme-modal" class="modal fade">

</div>

<script>
jQuery(function($){

$('a[data-toggle=modal]').click(function(event){
	var $this = $(event.target);
	var $modal = $($this.data('target'));
	$modal.load($this.attr('href'));
});

$("a.disabled").on('click', function(e){e.preventDefault(e); return false;})

});
</script>