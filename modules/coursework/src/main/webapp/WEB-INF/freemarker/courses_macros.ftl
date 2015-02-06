<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<#macro autoGradeOnline gradePath gradeLabel markPath markingId>
	<@form.label path="${gradePath}">${gradeLabel}</@form.label>
	<@form.field>
		<@f.input path="${gradePath}" cssClass="input-small auto-grade" id="auto-grade-${markingId}" />
		<select name="${gradePath}" class="input-small" disabled style="display:none;"></select>
		<@fmt.help_popover id="auto-grade-${markingId}-help" content="The grades available depends on the mark entered and the SITS mark scheme in use" />
		<@f.errors path="${gradePath}" cssClass="error" />
	</@form.field>
	<script>
		jQuery(function($){
			var $gradeInput = $('#auto-grade-${markingId}').hide()
				, $markInput = $gradeInput.closest('form').find('input[name=${markPath}]')
				, $select = $gradeInput.closest('div').find('select').on('click', function(){
					$(this).closest('.control-group').removeClass('info');
				})
				, currentRequest = null
				, data = {'studentMarks': {}}
				, doRequest = function(){
					if (currentRequest != null) {
						currentRequest.abort();
					}
					data['studentMarks']['${markingId}'] = $markInput.val();
					if ($select.is(':visible') || $gradeInput.val().length > 0) {
						data['selected'] = {};
						data['selected']['${markingId}'] = ($select.is(':visible')) ? $select.val() : $gradeInput.val();
					}
					currentRequest = $.ajax('<@routes.generateGradesForMarks command.assignment />',{
						'type': 'POST',
						'data': data,
						success: function(data) {
							$select.html(data);
							if ($select.find('option').length > 1) {
								$gradeInput.hide().prop('disabled', true);
								$select.prop('disabled', false).show()
									.closest('.control-group').addClass('info');
								$('#auto-grade-${markingId}-help').show();
							} else {
								$gradeInput.show().prop('disabled', false);
								$select.prop('disabled', true).hide();
								$('#auto-grade-${markingId}-help').hide();
							}
						}, error: function(xhr, errorText){
							if (errorText != "abort") {
								$gradeInput.show().prop('disabled', false);
								$('#auto-grade-${markingId}-help').hide();
							}
						}
					});
				};
			$markInput.on('keyup', doRequest);
			doRequest();
		});
	</script>
</#macro>

<#macro marksForm assignment templateUrl formUrl commandName cancelUrl>
	<div id="batch-feedback-form">
		<h1>Submit marks for ${assignment.name}</h1>
		<ul id="marks-tabs" class="nav nav-tabs">
			<li class="active"><a href="#upload">Upload</a></li>
			<li class="webform-tab"><a href="#webform">Web Form</a></li>
		</ul>
		<div class="tab-content">
			<div class="tab-pane active" id="upload">
				<p>
					You can upload marks in a spreadsheet, which must be saved as an .xlsx file (ie created in Microsoft Office 2007 or later).
					The spreadsheet should have three column headings in the following order: <b>ID, Mark, Grade</b>.
					You can use this <a href="${templateUrl}" >generated spreadsheet</a> as a template.
					Note that you can upload just marks, just grades or both.
				</p>
				<@f.form method="post" enctype="multipart/form-data" action="${formUrl}" commandName="${commandName}">
					<input name="isfile" value="true" type="hidden"/>
					<table role="presentation" class="narrowed-form">
						<tr>
							<td id="multifile-column">
								<h3>Select file</h3>
								<p id="multifile-column-description">
									<#include "/WEB-INF/freemarker/multiple_upload_help.ftl" />
								</p>
								<@form.labelled_row "file.upload" "Files">
									<input type="file" name="file.upload" multiple />
								</@form.labelled_row>
								<@f.errors path="file" cssClass="error" />
							</td>
						</tr>
					</table>
					<div class="submit-buttons">
						<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
					</div>
				</@f.form>
			</div>
			<div class="tab-pane " id="webform">
				<p>
					Click the add button below to enter marks for a student.
				</p>
				<table class="hide">
					<tbody class="row-markup">
					<tr class="mark-row">
						<td>
							<div class="input-prepend input-append">
								<span class="add-on"><i class="icon-user"></i></span>
								<input class="universityId span2" name="universityId" type="text" />
							</div>
						</td>
						<td><input name="actualMark" type="text" /></td>
						<td>
							<input class="grade input-small" name="actualGrade" type="text"/>
							<#if isGradeValidation>
								<select name="actualGrade" class="input-small" disabled style="display:none;"></select>
							</#if>
						</td>
					</tr>
					</tbody>
				</table>
				<@f.form id="marks-web-form" method="post" enctype="multipart/form-data" action="${formUrl}" commandName="${commandName}">
					<div class="fix-area">
						<input name="isfile" value="false" type="hidden"/>
						<table class="marksUploadTable">
							<tr class="mark-header">
								<th>University ID</th>
								<th>Marks</th>
								<th>Grade <#if isGradeValidation><@fmt.help_popover id="auto-grade-help" content="The grade is automatically calculated from the SITS mark scheme" /></#if></th>
							</tr>
							<#if marksToDisplay??>
								<#list marksToDisplay as markItem>
									<tr class="mark-row">
										<td>
											<div class="input-prepend input-append">
												<span class="add-on"><i class="icon-user"></i></span>
												<input class="universityId span2" value="${markItem.universityId}" name="marks[${markItem_index}].universityId" type="text" readonly="readonly" />
											</div>
										</td>
										<td><input name="marks[${markItem_index}].actualMark" value="<#if markItem.actualMark??>${markItem.actualMark}</#if>" type="text" /></td>
										<td>
											<input name="marks[${markItem_index}].actualGrade" class="grade input-small" value="<#if markItem.actualGrade??>${markItem.actualGrade}</#if>" type="text" />
											<#if isGradeValidation>
												<select name="marks[${markItem_index}].actualGrade" class="input-small" disabled style="display:none;"></select>
											</#if>
										</td>
									</tr>
								</#list>
							</#if>
						</table>
						<br /><button class="add-additional-marks btn"><i class="icon-plus"></i> Add</button>
						<div class="submit-buttons fix-footer">
							<input type="submit" class="btn btn-primary" value="Save">
							or <a href="${cancelUrl}" class="btn">Cancel</a>
						</div>
					</div>
				</@f.form>
			</div>
		</div>
	</div>

	<script>
		jQuery(function($){
			$('.fix-area').fixHeaderFooter();
			// Fire a resize to get the fixed button in the right place
			$('.webform-tab').on('shown', function(){
				$(window).trigger('resize');
			});

			if (${isGradeValidation?string('true','false')}) {
				var currentRequest = null, doIndividualRequest = function() {
					if (currentRequest != null) {
						currentRequest.abort();
					}
					var data = {'studentMarks': {}, 'selected': {}}
						, $this = $(this)
						, $markRow = $this.closest('tr')
						, $gradeInput = $markRow.find('input.grade')
						, $select = $markRow.find('select')
						, universityId = $markRow.find('input.universityId').val();
					data['studentMarks'][universityId] = $this.val();
					if ($select.is(':visible') || $gradeInput.val().length > 0) {
						data['selected'] = {};
						data['selected'][universityId] = ($select.is(':visible')) ? $select.val() : $gradeInput.val();
					}
					currentRequest = $.ajax('<@routes.generateGradesForMarks assignment />',{
						'type': 'POST',
						'data': data,
						success: function(data) {
							$select.html(data);
							if ($select.find('option').length > 1 || $this.val() == "") {
								$gradeInput.hide().prop('disabled', true);
								$select.prop('disabled', false).show()
							} else {
								$gradeInput.show().prop('disabled', false);
								$select.prop('disabled', true).hide();
							}
						}, error: function(xhr, errorText){
							if (errorText != "abort") {
								$gradeInput.show().prop('disabled', false);
							}
						}
					});
				};

				$('.marksUploadTable').on('keyup', 'input[name*="actualMark"]', doIndividualRequest).on('tableFormNewRow', function(){
					// Make sure all the selects have the correct name
					$('.marksUploadTable .mark-row select').each(function(){
						$(this).prop('name', $(this).closest('td').find('input').prop('name'));
					});
				});

				var currentData = {'studentMarks': {}, 'selected': {}};
				var $markRows = $('.marksUploadTable .mark-row').each(function(){
					var $markRow = $(this)
						, universityId = $markRow.find('input.universityId').val()
						, $select = $markRow.find('select')
						, $gradeInput = $markRow.find('input.grade');
					currentData['studentMarks'][universityId] = $markRow.find('input[name*="actualMark"]').val();
					currentData['selected'][universityId] = ($select.is(':visible')) ? $select.val() : $gradeInput.val();
					$gradeInput.hide();
				});
				$.ajax('<@routes.generateGradesForMarks assignment />/multiple',{
					'type': 'POST',
					'data': currentData,
					success: function(data) {
						var $selects = $(data);
						$markRows.each(function(){
							var $markRow = $(this)
								, universityId = $markRow.find('input.universityId').val()
								, $thisSelect = $selects.find('select').filter(function(){
									return $(this).data('universityid') == universityId;
								});
							if ($thisSelect.length > 0) {
								$markRow.find('input.grade').hide().prop('disabled', true);
								$markRow.find('select').html($thisSelect.html()).prop('disabled', false).show();
							} else {
								$markRow.find('input.grade').show().prop('disabled', false);
								$markRow.find('select').prop('disabled', true).hide();
							}

						});
					}
				});
			}
		});
	</script>
</#macro>