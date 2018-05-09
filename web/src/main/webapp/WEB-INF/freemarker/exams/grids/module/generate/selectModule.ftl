<#import 'form_fields.ftl' as form_fields />
<#escape x as x?html>

<#function route_function dept>
	<#local selectModuleCommand><@routes.exams.generateModuleGrid dept academicYear /></#local>
	<#return selectModuleCommand />
</#function>

<@fmt.id7_deptheader title="Create a new module exam grid for ${department.name}" route_function=route_function />

<h2>Select grid options</h2>

<p class="progress-arrows">
	<span class="arrow-right active">Select modules</span>
	<span class="arrow-right arrow-left">Preview and download</span>
</p>

<div class="alert alert-info">
	<h3>Before you start</h3>
	<p>Exam grids in Tabula are generated using data stored in SITS.
		Before you create a new grid, ensure you have entered all the necessary data in SITS and verified its accuracy.</p>
</div>

<form action="<@routes.exams.generateModuleGrid department academicYear />" class="form-inline select-course" method="post">
	<div class="form-inline creation-form" style="margin-bottom: 10px;">
		<label for="module-picker">Select module:</label>
		<select id="module-picker" name ="module" class="form-control" style="width: 360px;" placeholder="Start typing a module code or name&hellip;">
			<option value=""></option>
			<#list modules as module>
				<option value="${module.code}"><@fmt.module_name module false /></option>
			</#list>
		</select>

		<@bs3form.errors path="selectModuleExamCommand" />

		<div class="buttons">
			<button class="btn btn-default disabled" type="submit" name="${GenerateModuleExamGridMappingParameters.selectModule}" >Next</button>
		</div>
	</div>
</form>

<script>
	jQuery(function($){
		var $picker = $('.creation-form #module-picker');
		var $button = $('.creation-form .buttons .btn');
		$picker.comboTypeahead();

		var manageButtonState = function(value) {
			if (value) {
				$button.removeClass('disabled').addClass('btn-primary');
			} else {
				$button.addClass('disabled').removeClass('btn-primary');
			}
		};
		manageButtonState($picker.find(':selected').val());

		$picker.on('change', function() {
			var value = $(this).find(':selected').val();
			manageButtonState(value);
		});
	});
</script>
</#escape>