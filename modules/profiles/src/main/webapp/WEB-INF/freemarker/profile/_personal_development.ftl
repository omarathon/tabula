<#escape x as x?html>
<section id="personal-development" class="clearfix">
	<h4>Personal tutor</h4>

	<#if profile.personalTutor?is_string>
		<p>
			${profile.personalTutor}
			<#if !profile.personalTutor?string?starts_with("Not ")>
				<span class="muted">External to Warwick</span>
			</#if>
			<#if can.do("Profiles.PersonalTutor.Update", profile)>
				<a id="edit-tutor-link" href="<@routes.tutor_edit_no_tutor student=profile.universityId />"><i class="icon-edit"></i></a>
			</#if>
		</p>
	<#else>
		<div class="tutor clearfix">
			<div class="photo">
				<img src="<@routes.tutorPhoto profile />" />
			</div>
			<h5>
				${profile.personalTutor.fullName}
				<#if can.do("Profiles.PersonalTutor.Update", profile)>
					<a id="edit-tutor-link" href="<@routes.tutor_edit student=profile.universityId tutor=profile.personalTutor/>"><i class="icon-edit"></i></a>
				</#if>
			</h5>
			<#if profile.personalTutor.universityId == viewer.universityId>
				<span class="muted">(you)</span>
			<#else>
				<#if profile.personalTutor.email??>
					<p><i class="icon-envelope"></i> <a href="mailto:${profile.personalTutor.email}">${profile.personalTutor.email}</a></p>
				</#if>
			</#if>
		</div>
	</#if>

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
			var $f = $(frame).contents();

			if ($f.find("#meeting-record-form").length == 1) {
				$f.find("input.date-picker").tabulaDatePicker();
				var wait = setInterval(function() {
					var h = $f.find("body").height();
					if (h > 0) {
						clearInterval(wait);
						$m.find(".modal-body").animate({ height: h });
					}
				}, 50);
				$m.modal("show");
				$m.on("shown", function() {
					$f.find("[name='title']").focus();
				});
			} else if ($f.find("section.meetings").length == 1) {
				$("section.meetings").replaceWith($f.find("section.meetings"));
				$('details').details();
				$m.modal("hide");
				scrollToOpenDetails();
			} else {
				$m.modal('hide');
				// TODO more user-friendly fall-back
				document.location.reload(true);
			}
		}

		// run at start
		scrollToOpenDetails();

		// load form into modal, with picker enabled
		$("#personal-development").on("click", "section.meetings .new", function(e) {
			e.preventDefault();
<<<<<<< HEAD
			var target = $(this).attr("href");

			$m.load(target + "?modal", function() {
				var $mb = $m.find(".modal-body").empty();
				$('<iframe frameBorder="0" scrolling="no" style="height:100%;width:100%;" id="#modal-content"></iframe>')
					.load(function() {
						frameLoad(this);
					})
					.attr("src", target + "?iframe")
					.appendTo($mb);
=======
			
			$m.load($(this).attr("href") + "?modal", function() {
				$m.modal("show");
				$m.on("shown", function() {
					$m.find("[name='title']").focus();
				});
>>>>>>> develop
			});
		});

		$m.on('click', 'button[type=submit]', function(e){
			e.preventDefault();
<<<<<<< HEAD
			$m.find('iframe').contents().find('form').submit();
=======
			var $form = $(this);
			
			$.ajax({
				type: $form.attr("method"),
				url: $form.attr("action"),
				data: $form.serialize(),
				
				error: function() {
					// just close
					$m.modal("hide");
				},
				
				success: function(data, status) {
					thing = data;
					if (data.indexOf("modal-header") > -1) {
						// reload into modal
						$m.html(data);
						$m.find("input.date-picker").tabulaDatePicker();
						$m.find('form.double-submit-protection').tabulaSubmitOnce();
					} else {
						// reload meeting data
						$("section.meetings").replaceWith(data);
						$('details').details();
						$m.modal("hide");
						scrollToOpenDetails();
					}
					$m.tabulaPrepareSpinners();
				}
			});
			
>>>>>>> develop
		});
	});
	</script>
</section>
</#escape>