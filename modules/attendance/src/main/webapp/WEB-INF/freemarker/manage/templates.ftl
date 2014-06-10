<h3>Use a template</h3>

<p>Use an AQA-approved template to add points to
	<a href="#" class="use-popover"
	   data-content="
		<ul>
			<#list schemes as scheme>
				<li>${scheme.name}</li>
			</#list>
		</ul>"
	   data-html="true"
	   data-placement="right"><@fmt.p schemes?size "scheme" /></a>.

	Which template do you want to use?</p>

<@f.form action="" method="POST" commandName="command" class="form-horizontal">

	<#list schemes as scheme>
		    <@f.hidden path="schemes"/>
	</#list>

	<@form.labelled_row "templateScheme" "Template:">
		<@f.select path="templateScheme" id="templateSchemeSelect">
			<@f.option value="" label="No template"/>
			<#list templates as template>
				<@f.option value="${template.id}" label="${template.templateName}"/>
			</#list>
		</@f.select>
	</@form.labelled_row>

	<div id="templatePoints"></div>

	<div class="submit-buttons fix-footer">
		<button class="btn btn-primary" type="submit" value="submit">Apply</button>
		<a class="btn" href="<@routes.manageAddPoints department academicYear />">Cancel</a>
	</div>

</@f.form>

<script>
	//TODO make this better.
	(function ($) {
		$('#templateSchemeSelect').change(function(){
			var templateSchemeId = $(this).val();

			$('.fix-footer').removeAttr('style').attr('class', 'submit-buttons fix-footer');
			$('.footer-shadow').remove();

			if(templateSchemeId != '') {
				$.get(window.location.pathname + '/' + templateSchemeId,
				function(data){
					$("#templatePoints").html(data);
					$('body').fixHeaderFooter();
				})
			} else {
				$("#templatePoints").html("");
			}
		})
	})(jQuery);
</script>
