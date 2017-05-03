<div class="reusable-workflow-picker workflow-fields">
	<@bs3form.labelled_form_group path="reusableWorkflow" labelText="Workflow name">
		<@f.select path="reusableWorkflow" class="form-control" >
			<option value="" <#if !status.value??>selected</#if> disabled></option>
			<@f.options items=reusableWorkflows itemValue="id" itemLabel="name" />
		</@f.select>
	</@bs3form.labelled_form_group>
</div>

<div class="single-use-workflow-fields workflow-fields">
	<#include "../workflows/_shared_fields.ftl" />
</div>

<script type="text/javascript">
	(function ($) { "use strict";

		$('select[name=workflowCategory]').on('change', function() {
			var $this = $(this);
			var value = $this.val();
			$('.workflow-fields').hide();
			if(value === "R") {
				$('.reusable-workflow-picker').show();
			} else if (value === "S") {
				$('.single-use-workflow-fields').show()
			}
		}).trigger('change');

	})(jQuery);
</script>