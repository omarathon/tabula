<div class="row">
	<div class="span12">
		<form class="template form-inline" action="<@url page="/manage/${departmentCode}/sets/edit"/>">
			<input type="hidden" name="set" value="${pointSet.id}" />
			<input type="hidden" name="route" value="${pointSet.route.code}" />
			<label>Template name
				<select name="templateName">
					<#assign templates = [
						"1st year Undergraduate"
                    	"2nd year Undergraduate",
                    	"3rd year Undergraduate",
						"4th year Undergraduate",
						"Postgraduate taught",
						"Postgraduate research"
					]>
					<#assign foundName = false>
					<#list templates as template>
						<option value="${template}" <#if pointSet.templateName == template><#assign foundName = true>selected</#if>>${template}</option>
					</#list>
					<#if foundName == false>
						<option value="${pointSet.templateName}" selected>${pointSet.templateName}</option>
					</#if>
					<option value="">Other&hellip;</option>
				</select>
				<input type="text" name="customTemplateName" />
			</label>
			<input class="btn btn-primary spinnable spinner-auto" type="submit" value="Update" data-loading-text="Updating&hellip;"/>
			<span class="error" style="display: inline;"></span>
		</form>
	</div>
</div>

<script>
	jQuery(function($){
		$('form.template')
			.find('select[name="templateName"]').on('change',function(){
				if($(this).val().length === 0) {
					$(this).hide().siblings('input[name="customTemplateName"]').show().focus();
				}
			}).end()
			.find('input[name="customTemplateName"]').hide();
	});
	Attendance.Manage.bindUpdateSetForm();
</script>

<#macro pointsInATerm term>
	<div class="striped-section">
		<h2 class="section-title">${term}</h2>
		<div class="striped-section-contents">
			<#list pointsByTerm[term]?sort_by("week") as point>
				<div class="item-info row-fluid point">
					<div class="span12">
						<div class="pull-right">
							<a class="btn btn-primary edit" href="<@url page="/manage/${departmentCode}/points/edit?set=${point.pointSet.id}&point=${point.id}"/>">Edit</a>
							<a class="btn btn-danger delete" href="<@url page="/manage/${departmentCode}/points/delete?set=${point.pointSet.id}&point=${point.id}"/>">Delete</a>
						</div>
						${point.name} (<@fmt.weekRanges point />)
					</div>
				</div>
			</#list>
		</div>
	</div>
</#macro>

<#if pointSet.points?size == 0>
	<div class="row-fluid">
		<div class="span12">
			<em>No points exist for this set</em>
		</div>
	</div>
<#else>
	<#list ["Term 1", "Christmas vacation", "Term 2", "Spring vacation", "Term 3", "Summer vacation"] as term>
		<#if pointsByTerm[term]??>
			<@pointsInATerm term/>
		</#if>
	</#list>
</#if>

<script>
	Attendance.Manage.showAndBindNewPointButton();
	Attendance.Manage.bindEditAndDeletePointButtons();
</script>