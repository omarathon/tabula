/**
 * Scripts used only by student profiles.
 */
(function ($) { "use strict";

	var exports = {};

	$(function() {
		$('.profile-search').each(function() {
			var container = $(this);

			var target = container.find('form').prop('action') + '.json';

			var xhr = null;
			container.find('input[name="query"]').prop('autocomplete','off').each(function() {
				var $spinner = $('<div class="spinner-container" />');
				$(this).before($spinner);

				$(this).typeahead({
					source: function(query, process) {
						if (xhr != null) {
							xhr.abort();
							xhr = null;
						}

						query = $.trim(query);
						if (query.length < 3) { process([]); return; }

						// At least one of the search terms must have more than 1 character
						var terms = query.split(/\s+/g);
						if ($.grep(terms, function(term) { return term.length > 1; }).length == 0) {
							process([]); return;
						}

						$spinner.spin('small');
						xhr = $.get(target, { query : query }, function(data) {
							$spinner.spin(false);

							var members = [];

							$.each(data, function(i, member) {
								var item = member.name + "|" + member.id + "|" + member.userId + "|" + member.description;
								members.push(item);
							});

							process(members);
						}).error(function(jqXHR, textStatus, errorThrown) { if (textStatus != "abort") $spinner.spin(false); });
					},

					matcher: function(item) { return true; },
					sorter: function(items) { return items; }, // use 'as-returned' sort
					highlighter: function(item) {
						var member = item.split("|");
						return '<img src="/profiles/view/photo/' + member[1] + '.jpg?size=tinythumbnail" class="photo pull-right"><h3 class="name">' + member[0] + '</h3><div class="description">' + member[3] + '</div>';
					},

					updater: function(item) {
						var member = item.split("|");
						window.location = '/profiles/view/' + member[1];

						return member[0];
					},
					minLength:3
				});
			});
		});
	});

	// take anything we've attached to "exports" and add it to the global "Profiles"
	// we use extend() to add to any existing variable rather than clobber it
	window.Profiles = jQuery.extend(window.Profiles, exports);

	// MEETING RECORD STUFF
	$(function() {
		function scrollToOpenDetails() {
			// prevent js errors when getNavigationHeight is undefined
			if(window.getNavigationHeight != undefined){
				$("details.open").each(function() {
					$("html, body").animate({
						scrollTop: $(this).offset().top - window.getNavigationHeight()
					}, 300);
				});
			}
		};

		function frameLoad(frame) {
			var $m = $("#modal");
			var $f = $(frame).contents();

			// reset slow load spinner
			$m.tabulaPrepareSpinners();

			if ($f.find("#meeting-record-form").length == 1) {
				// unhide the iframe
				$m.find('.modal-body').slideDown();

				// reset datepicker & submit protection
				var $form = $m.find('form.double-submit-protection');
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
                var source =$f.find("section.meetings");
                var targetClass = source.attr('data-target-container');
                var target = $("section."+targetClass);
				// bust the returned content out to the original page, and kill the modal
				target.replaceWith(source);
				$('details').details();
				// rebind all of this stuff to the new UI
				decorateMeetingRecords();
				$m.modal("hide");
			} else {
				/*
					TODO more user-friendly fall-back?
					This is where you end up with an unexpected failure, eg. permission failure, mangled URL etc.
					The default is to reload the original profile page. Not sure if there's something more helpful
					we could/should do here.
				*/
				$m.modal('hide');
				document.location.reload(true);
			}
		}


		function decorateMeetingRecords(){

			// delete meeting records
			$('a.delete-meeting-record').on('click', function(e) {
				var $this = $(this);
				var $details = $this.closest('details');

				if (!$details.hasClass("deleted")) {
					$details.addClass("processing");
					var url = $this.attr("href");
					$.post(url, function(data) {
						if (data.status == "successful") {
							$details.addClass("deleted muted");
						}
						$details.removeClass("processing");
					}, "json");
				}
				return false;
			});

			// restore meeting records
			$('a.restore-meeting-record').on('click', function(e) {
				var $this = $(this);
				var $details = $this.closest('details');

				if ($details.hasClass("deleted")) {
					$details.addClass("processing");
					var url = $this.attr("href");
					$.post(url, function(data) {
						if (data.status == "successful") {
							$details.removeClass("deleted muted");
						}
						$details.removeClass("processing");
					}, "json");
				}
				return false;
			});

			// purge meeting records
			$('a.purge-meeting-record').on('click', function(e) {
				var $this = $(this);
				var $details = $this.closest('details');

				if ($details.hasClass("deleted")) {
					$details.addClass("processing");
					var url = $this.attr("href");
					$.post(url, function(data) {
						if (data.status == "successful") {
							$details.remove();
						}
					}, "json");
				}
				return false;
			});

			// show rejection comment box
			var rejectRadios = $('input.reject').each( function() {
				var $this = $(this);
				var $form = $this.closest('form');
				var $commentBox = $form.find('.rejection-comment');
				$this.slideMoreOptions($commentBox, true);
			});

			// make modal links use ajax
			$('section.meetings').ajaxSubmit(function() {
				document.location.reload(true);
			});

			var $m = $("#modal");

			scrollToOpenDetails();

			/* load form into modal, with picker enabled
			 * click selector must be specific otherwise the click event will propagate up past the section element
			 * these HTML5 elements have default browser driven behaviour that is hard to override.
			 */
			var meetingsSection = $("section.meetings");
			$(".new, .edit-meeting-record", meetingsSection).on("click", function(e) {
				var target = $(this).attr("href");

				$m.load(target + "?modal", function() {
					var $mb = $m.find(".modal-body").empty();
					var iframeMarkup = "<iframe frameBorder='0' scrolling='no' style='height:100%;width:100%;' id='modal-content'></iframe>";
					$(iframeMarkup)
						.load(function() {
							frameLoad(this);
						})
						.attr("src", target + "?iframe")
						.appendTo($mb);
				});
				return false;
			});

			$m.on('submit', 'form', function(e){
				e.preventDefault();
				// submit the inner form in the iframe
				$m.find('iframe').contents().find('form').submit();

				// hide the iframe, so we don't get a FOUC
				$m.find('.modal-body').slideUp();
			});

		};
		// call on page load
		decorateMeetingRecords();


	});
	//MEETING RECORD APPROVAL STUFF

}(jQuery));
