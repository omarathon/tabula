(function($){ "use strict";

/* fixHeaderFooter plugin
	 *
	 * apply this to a container div
	 * which should contain a .persist-header and/or a .persist-footer
	 * it'll fix the header at the top when scrolled
	 * and it'll fix the footer at the bottom.
	 * (CSS in main.less)
	 */

	$.fn.visible = function() {
		return this.css('visibility', 'visible');
	};

	$.fn.invisible = function() {
		return this.css('visibility', 'hidden');
	};

	$.fn.fixHeaderFooter = function() {

		var areaToPersist = this;

		var updateTableHeaders = function(persistArea) {

			var el               = $(persistArea),
				offset           = el.offset(),
				scrollTop        = $(window).scrollTop(),
				floatingHeader   = $(".floatingHeader", persistArea),
				floatingFooter   = $(".floatingFooter", persistArea),
				persistHeader    = $(".persist-header", persistArea),
				persistFooter    = $(".persist-footer", persistArea),
				primaryNavHeight = $("#primary-navigation").height();

			if ((scrollTop > offset.top) && (scrollTop < offset.top + el.height())) {
				floatingHeader.visible();
			} else {
				floatingHeader.invisible();
			}

			// persistFooter will need to have a margin-bottom of zero
			// otherwise you'll see the original footer (and bits of the webpage) underneath the fixed footer as you scroll
			if(persistFooter.offset().top < $(window).scrollTop() + $(window).height() - primaryNavHeight - persistFooter.height()) {
				floatingFooter.invisible();
			} else {
				floatingFooter.visible();
			}

		}

		var cloneRow = function(row, className) {
			row.before(row.clone(true))
				.css("width", row.width())
				.addClass(className);
		};


		$(this).each(function() {
			if($(".persist-header").length) cloneRow($(".persist-header", this), "floatingHeader");
			if($(".persist-footer").length) cloneRow($(".persist-footer", this), "floatingFooter");
		});


		$(window).scroll(function() {
			updateTableHeaders(areaToPersist);
		});



		// public methods
		this.initialize = function() {
			return this;
		};


 		// method to fix the jumbo direction icon in place
		this.fixDirectionIcon = function() {
			var directionIcon = $('.direction-icon');
			var fixContainer = $('.fix-on-scroll-container');
			var persistAreaTop = $('.persist-header').height() + $('#primary-navigation').height();

			if(fixContainer.offset().top - $(window).scrollTop() < $('.persist-header').height() + $('#primary-navigation').height()) {
				directionIcon.css({ "top" : persistAreaTop, "position": "fixed", "width": directionIcon.width() });
			} else {
				directionIcon.css({"top": "auto", "position": "static", "width": directionIcon.width });
			}
		};

		// if the list of agents is shorter than the (viewport+fixed screen areas)
		// and we've scrolled past the top of the persist-area container, then fix it
		// (otherwise don't, because the user won't be able to see all of the items in the well)
		this.fixTargetList = function(listToFix) {
			var targetList = $(listToFix);
			var persistAreaTop = $('.persist-header').height() + $('#primary-navigation').height();
			var viewableArea = $(window).height() - ($('.persist-header').height() + $('#primary-navigation').height() + $('.persist-footer').height());

			// width is set on fixing because it was jumping to the minimum width of the content
			if (targetList.height() < viewableArea && ($(window).scrollTop() > $('.persist-area').offset().top)) {
				targetList.css({"top": persistAreaTop + 14, "position": "fixed", "width": targetList.parent().width()});
			} else {
				targetList.css({"top": "auto", "position": "relative", "width": "auto"});
			}
		}

		this.viewableArea = function() {
			return $(window).height() - ($('.persist-header').height() + $('#primary-navigation').height() + $('.persist-footer').outerHeight());
		}


		return this.initialize();


	} // end fixHeaderFooter plugin

})(jQuery);