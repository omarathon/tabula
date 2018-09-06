<#if field.name == "notes">
	<#assign rows = "3" />
	<#assign helpText>
		Notes are visible to markers, moderators and administrators for this assignment. Notes are not normally shared with students.
	</#assign>
<#else>
	<#assign rows = "6" />
	<#assign helpText>
		You can use markdown syntax <a target="_blank" href="https://warwick.ac.uk/services/its/servicessupport/web/tabula/manual/cm2/markers/markdown/"><i class="icon-question-sign fa fa-question-circle"></i></a>
	</#assign>
</#if>
<@bs3form.labelled_form_group "fields[${field.id}].value" field.label help>
	<@form.field>
		<@f.textarea id="fields[${field.id}].value" cssClass="form-control" path="fields[${field.id}].value" rows=rows />
	</@form.field>
	<script>
		jQuery(function ($) {
			var previewBlock = $("#preview-block-${field.id}-${command.student.userId}");
			var feedbackTextArea = $("form[studentid=${command.student.userId}] textarea");
			var previewText = $("div[id='${field.id}-${command.student.userId}-preview-content']");

			function trimPTag(inputString) {
				return inputString.replace('<p>', '').replace('</p>', '').trim();
			}

			// do not render the preview block if they look the same
			function hidePreviewIfNotNeeded() {
				if (trimPTag(previewText.html()) === feedbackTextArea.val().trim()) {
					previewBlock.hide();
				} else {
					previewBlock.show();
				}
			}

			previewBlock.hide();

			var timeout = null;
			feedbackTextArea.keyup(function () {
				if (timeout != null) {
					clearTimeout(timeout);
				}
				timeout = setTimeout(function () {
					timeout = null;
					if (trimPTag(previewText.html()) !== feedbackTextArea.val().trim()) {
						$.ajax('/markdown/toHtml', {
							'type': 'POST',
							'data': {
								markdownString: feedbackTextArea.val()
							},
							success: function (res) {
								// update content as long as they are different
								if (res.trim() !== previewText.html().trim()) {
									previewText.html(res);
								}
								hidePreviewIfNotNeeded();
							}
						});
					}
				}, 300);
			});
		});
	</script>
	<#if showHelpText?? && showHelpText>
		<div class="help-block">${helpText}</div>
	</#if>
</@bs3form.labelled_form_group>

<div id="preview-block-${field.id}-${command.student.userId}">
	<label>Preview</label>
	<div class="well">
		<div id="${field.id}-${command.student.userId}-preview-content"></div>
	</div>
</div>