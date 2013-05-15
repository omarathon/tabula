<#escape x as x?html>
<section id="personal-development" class="clearfix">
	<#if profile.personalTutors??>
		<h4>Personal tutor<#if profile.personalTutors?size gt 1>s</#if></h4>
		
		<#if profile.personalTutors?size == 0>
			<p>
				Not recorded
				<#if can.do("Profiles.PersonalTutor.Update", profile) && (profile.studyDetails.studyDepartment)?? && profile.studyDetails.studyDepartment.canEditPersonalTutors >
					<a id="edit-tutor-link" href="<@routes.tutor_edit_no_tutor student=profile.universityId />"><i class="icon-edit"></i></a>
				</#if>
			</p>
		</#if>
	
		<div class="tutors clearfix row">
		<#list profile.personalTutors as relationship>
			<#assign personalTutor = relationship.agentMember />
			<div class="tutor clearfix span4">
				<#if !personalTutor??>
					${relationship.agentName} <span class="muted">External to Warwick</span>
					<#if can.do("Profiles.PersonalTutor.Update", profile) && (profile.studyDetails.studyDepartment)?? && profile.studyDetails.studyDepartment.canEditPersonalTutors >
						<a id="edit-tutor-link" href="<@routes.tutor_edit_no_tutor student=profile.universityId />"><i class="icon-edit"></i></a>
					</#if>
				<#else>
					<div class="photo">
						<img src="<@routes.relationshipPhoto profile relationship />" />
					</div>
					<h5>
						${personalTutor.fullName!"Personal tutor"}
						<#if can.do("Profiles.PersonalTutor.Update", profile) && (profile.studyDetails.studyDepartment)?? && profile.studyDetails.studyDepartment.canEditPersonalTutors >
							<a id="edit-tutor-link" href="<@routes.tutor_edit student=profile.universityId currentTutor=personalTutor/>"><i class="icon-edit"></i></a>
						</#if>
					</h5>
					<#if personalTutor.universityId == viewer.universityId>
						<span class="muted">(you)</span>
					<#else>
						<#if personalTutor.email??>
							<p><i class="icon-envelope"></i> <a href="mailto:${personalTutor.email}">${personalTutor.email}</a></p>
						</#if>
					</#if>
				</#if>
			</div>
		</#list>
		</div>
		
		<#if profile.hasAPersonalTutor>
			<#include "../tutor/meeting/list.ftl" />
		</#if>
	
		<div id="modal" class="modal hide fade" style="display:none;">
			<div class="modal-header"></div>
			<div class="modal-body"></div>
			<div class="modal-footer"></div>
		</div>
	
	
		<script type="text/javascript">
		jQuery(function($){
			var $m = $("#modal");
	
			var scrollToOpenDetails = function() {
				$("details.open").each(function() {
					$("html, body").animate({
						scrollTop: $(this).offset().top - window.getNavigationHeight()
					}, 300);
				});
			};
	
			frameLoad = function(frame) {
				// reset slow load spinner
				$m.tabulaPrepareSpinners();
	
				var $f = $(frame).contents();
				if ($f.find("#meeting-record-form").length == 1) {
					// unhide the iframe
					$m.find('.modal-body').slideDown();
	
					// reset datepicker & submit protection
					$f.find("input.date-picker").tabulaDatePicker();
					$form = $m.find('form.double-submit-protection');
					$form.tabulaSubmitOnce();
					$form.find(".btn").removeClass('disabled');
					// wipe any existing state information for the submit protection
					$form.removeData('submitOnceSubmitted');

					// firefox fix
					var wait = setInterval(function() {
						var h = $f.find("body").height();
						if (h > 0) {
							clearInterval(wait);
							$m.find(".modal-body").animate({ height: h });
						}
					}, 50);
	
					// show-time
					$m.modal("show");
					$m.on("shown", function() {
						$f.find("[name='title']").focus();
					});
				} else if ($f.find("section.meetings").length == 1) {
					// bust the returned content out to the original page, and kill the modal
					$("section.meetings").replaceWith($f.find("section.meetings"));
					$('details').details();
					$m.modal("hide");
					scrollToOpenDetails();
				} else {
					<#--
					TODO more user-friendly fall-back?
					This is where you end up with an unexpected failure, eg. permission failure, mangled URL etc.
					The default is to reload the original profile page. Not sure if there's something more helpful
					we could/should do here.
					-->
					$m.modal('hide');
					document.location.reload(true);
				}
			}
	
			// run at start
			scrollToOpenDetails();
	
			// load form into modal, with picker enabled
			$("#personal-development").on("click", "section.meetings .new", function(e) {
				e.preventDefault();
				var target = $(this).attr("href");
	
				$m.load(target + "?modal", function() {
					var $mb = $m.find(".modal-body").empty();
					$('<iframe frameBorder="0" scrolling="no" style="height:100%;width:100%;" id="modal-content"></iframe>')
						.load(function() {
							frameLoad(this);
						})
						.attr("src", target + "?iframe")
						.appendTo($mb);
				});
			});
	
			$m.on('submit', 'form', function(e){
				e.preventDefault();
				// submit the inner form in the iframe
				$m.find('iframe').contents().find('form').submit();
	
				// hide the iframe, so we don't get a FOUC
				$m.find('.modal-body').slideUp();
			});
		});
		</script>
	</#if>
</section>
</#escape>