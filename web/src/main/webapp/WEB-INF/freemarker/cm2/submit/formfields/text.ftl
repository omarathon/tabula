<@bs3form.labelled_form_group "fields[${field.id}].value" "Feedback" help>
	<@form.field>
		<@f.textarea id="fields[${field.id}].value" cssClass="big-textarea form-control" path="fields[${field.id}].value" />
	</@form.field>
	<script>
		jQuery(function ($) {
			var previewBlock = $("#preview-block-${field.id}-${command.student.userId}");
			var feedbackTextArea = $("form[studentid=${command.student.userId}] textarea");
			var previewText = $("div[id='${field.id}-${command.student.userId}-preview-content']");

			function trimPTag(inputString) {
				return inputString.replace('<p>', '').replace('</p>', '').trim();
			}

			previewBlock.hide();
			setInterval(function () {
				if (trimPTag(previewText.html()) !== feedbackTextArea.val().trim()) {
					$.ajax('/markdown/toHtml', {
						'type': 'POST',
						'data': {
							markdownString: feedbackTextArea.val()
						},
						success: function (data) {
							// update content as long as they are different
							if (data.html.trim() !== previewText.html().trim()) {
								previewText.html(data.html);
							}

							// do not render the preview block if they look the same
							if (trimPTag(previewText.html()) === feedbackTextArea.val().trim()) {
								previewBlock.hide();
							} else {
								previewBlock.show();
							}
						}
					});
				}
			}, 1000); // update every 1 sec
		});
	</script>
	<#if showHelpText?? && showHelpText>
		<div class="help-block">
			You can use markdown syntax (e.g. press Enter twice to make a new paragraph).
		</div>
	</#if>
</@bs3form.labelled_form_group>

<div id="preview-block-${field.id}-${command.student.userId}">
	<label>Preview</label>
	<div class="well">
		<div id="${field.id}-${command.student.userId}-preview-content"></div>
	</div>
</div>