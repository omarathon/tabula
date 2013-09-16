<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
	<div class="modal-header">
	<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	<h3>Create new student note for ${command.member.fullName}</h3>
	</div>

	<div class="modal-body">
	<@f.form id="member-note-form" method="post" enctype="multipart/form-data" action="" commandName="command" class="form-horizontal double-submit-protection">
		<@form.labelled_row "title" "Title">
			<@f.input type="text" path="title" cssClass="input-block-level" maxlength="255" placeholder="The title" />
		</@form.labelled_row>

		<@form.labelled_row "note" "Note">
			<@f.textarea path="note" cssClass="input-block-level" rows="5" maxlength="255" />
		</@form.labelled_row>

		<#assign fileTypes=command.attachmentTypes />
		<@form.filewidget basename="file" types=fileTypes />
	</@f.form>

	</div>

	<div class="modal-footer">
		<input id="member-note-save" type="submit" class="btn btn-primary" value="Save">
	</div>


<script>
	jQuery(document).ready(function($) {

		$('#member-note-save').click(function() {

		 if ($(this).hasClass("disabled")) return;
			$.post($("#member-note-form").prop('action'), $("#member-note-form").serialize(), function(data){
				if($(".error", data).length > 0){
					$('#note-modal').html(data)
				} else {
					var action = "noteAdded";
					var currentUrl = [location.protocol, '//', location.host, location.pathname].join('');    // url without query string
					window.location = currentUrl + "?action=" + action;
				}
			});
		});
	})
</script>


