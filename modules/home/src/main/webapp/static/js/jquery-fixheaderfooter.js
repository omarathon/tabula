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

	} // end fixHeaderFooter plugin

})(jQuery);