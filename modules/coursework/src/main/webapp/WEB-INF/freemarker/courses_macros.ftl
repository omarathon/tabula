<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<#macro autoGradeOnline gradePath gradeLabel markPath markingId>
	<@form.label path="${gradePath}">${gradeLabel}</@form.label>
	<@form.field>
		<@f.input path="${gradePath}" cssClass="input-small auto-grade" id="auto-grade-${markingId}" />
		<select name="${gradePath}" class="input-small" disabled style="dsplay:none;"></select>
		<@fmt.help_popover id="auto-grade-${markingId}-help" content="The grades available depends on the mark entered and the SITS mark scheme in use" />
	</@form.field>
	<script>
		jQuery(function($){
			var $gradeInput = $('#auto-grade-${markingId}').hide()
				, $markInput = $gradeInput.closest('form').find('input[name=${markPath}]')
				, $select = $gradeInput.closest('div').find('select').on('click', function(){
					$(this).closest('.control-group').removeClass('info');
				})
				, currentRequest = null
				, doRequest = function(){
					if (currentRequest != null) {
						currentRequest.abort();
					}
					currentRequest = $.ajax('<@routes.generateGradesForMarks command.assignment />',{
						'type': 'POST',
						'data': {
							'studentMarks': {
								'${markingId}': $markInput.val()
							},
							'selected': ($select.is(':visible')) ? $select.val() : $gradeInput.val()
						}, success: function(data) {
							$gradeInput.remove();
							$select.html(data).prop('disabled', false).show();
							if ($select.find('option').length > 1) {
								$select.closest('.control-group').addClass('info');
							}
						}, error: function(){
							$gradeInput.show();
						}
					});
				};
			$markInput.on('keyup', doRequest);
			doRequest();
		});
	</script>
</#macro>