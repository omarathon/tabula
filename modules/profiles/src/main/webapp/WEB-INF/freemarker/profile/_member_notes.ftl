<section id="membernote-details" class="clearfix membernote">
	<h4>Student Notes</h4>
	<a class="btn-like membernote_create" data-toggle="modal" data-target="#note-modal" href="<@routes.create_member_note profile/>" title="Create a new student note"><i class="icon-edit"></i> New student note</a>
	<#-- <#if action == ">A note was added.</#if> -->


	<#if RequestParameters.action?? && RequestParameters.action == "noteAdded">
		<div class="alert alert-success">
			<button type="button" class="close" data-dismiss="alert">&times;</button>
			<p>A note was added to <strong>${profile.firstName}'s</strong> profile.</p>
		</div>
	</#if>


</section>

