<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<#macro autoGradeOnline gradePath gradeLabel markPath markingId>
	<@form.label path="${gradePath}">${gradeLabel}</@form.label>
	<@form.field>
		<@f.input path="${gradePath}" cssClass="input-small auto-grade" id="auto-grade-${markingId}" disabled="true"/>
		<@fmt.help_popover id="auto-grade-${markingId}-help" content="The grade is automatically calculated from the SITS mark scheme" />
	</@form.field>
	<script>
		jQuery(function($){
			var $gradeInput = $('#auto-grade-${markingId}')
				, $markInput = $gradeInput.closest('form').find('input[name=${markPath}]')
				, currentRequest = null
				, doRequest = function(){
					if (currentRequest != null) {
						currentRequest.abort();
					}
					currentRequest = $.post('<@routes.generateGradesForMarks command.assignment />',{
						'studentMarks' : {
							'${markingId}' : $markInput.val()
						}
					}, function(data){
						if (data['${markingId}'] != undefined) {
							$gradeInput.val(data['${markingId}']);
						}
					});
				};
			$markInput.on('keyup', doRequest);
		});
	</script>
</#macro>