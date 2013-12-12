(function($){ "use strict";

/* fixHeaderFooter plugin
	 *
	 * apply this to a container div
	 * which should contain one or more divs called .persist-header and/or a single .persist-footer
	 * it'll fix the headers (in order) at the top when scrolled
	 * and it'll fix the footer at the bottom.
	 * (CSS in main.less)
	 *
	 * it has one option for the minimum screen height to fix on
	 * you can call it like this
	 *
	 * $('.fixed-container').fixHeaderFooter({minimumScreenHeightFix: 800});
	 *
	 *
	 */

	$.fn.visible = function() {
		return this.css('visibility', 'visible');
	};

	$.fn.invisible = function() {
		return this.css('visibility', 'hidden');
	};

	$.fn.fixHeaderFooter = function(options) {
		var defaults = {
			minimumScreenHeightFix : 0 // if 0 - fix areas at all screen sizes, if > 0 - only
		};

		var options = $.extend(defaults, options);
		var areaToPersist = this;

		// create container for all the floated headers
		var floatedHeaderContainer = $("<div id=\"floatedHeaderContainer\"></div>")
											.width(areaToPersist.width())
											.css("top", $('#primary-navigation').height());
		areaToPersist.prepend(floatedHeaderContainer);


		var updateTableHeaders = function(persistArea) {

			var el               = $(persistArea),
				offset           = el.offset(),
				scrollTop        = $(window).scrollTop(),
				floatingHeader   = $(".floatingHeader", persistArea),
				floatingFooter   = $(".floatingFooter", persistArea),
				persistFooter    = $(".persist-footer", persistArea),
				primaryNavHeight = $('#primary-navigation').height(),
				floatedHeaderContainer = $('#floatedHeaderContainer');

			if ((scrollTop > offset.top) && (scrollTop < offset.top + el.height())) {
                floatedHeaderContainer.visible();
                floatingHeader.visible();

			} else {
                floatedHeaderContainer.invisible();
				floatingHeader.invisible();

			}

			// persistFooter will need to have a margin-bottom of zero
			// otherwise you'll see the original footer (and bits of the webpage) underneath the fixed footer as you scroll
			if (persistFooter && persistFooter.length) {
				if(persistFooter.offset().top < $(window).scrollTop() + $(window).height() - primaryNavHeight - persistFooter.height()) {
					floatingFooter.invisible();
				} else {
					floatingFooter.visible();
				}
			}

		}

		var cloneRow = function(row, className) {
			row.before(row.clone(true))
				.css("max-width", row.width())
				.addClass(className);
		};

		var cloneTableHead = function(tableHead, className) {
			tableHead.addClass("originalTableHead");

			// get the parent table to wrap our cloned thead in
			var tableWrap = "<table class=\"floatingHeadTable " + tableHead.parent().attr("class") + "\"></table>";
			tableHead.parent().before(tableWrap);

			// get the column groups out of the table and clone them
			var colgroups = tableHead.parent().find("colgroup").clone();

			// create the floatingHeader wrapper
			var headerWrap = $("<div class=\"persist-header " + className + "\"></div>")
				.css("max-width", tableHead.parent().width());

			// clone our thead and add it into the new floating table with the cloned colgroups,
			// and wrap all that in our floating wrapper div
			var newTableHead =
				tableHead.before(tableHead.clone(true))
					.appendTo(".floatingHeadTable")
					.removeClass("persist-header originalTableHead")
					.parent().prepend(colgroups)
					.append("<tbody></tbody>")
					.wrap(headerWrap);

			// force th widths to the same as the original table
			setTableHeadWidths($(".originalTableHead"), newTableHead);
		}

		// add all headers to the floated header container
		var stackHeaders = function(areaToPersist) {
			$(areaToPersist).find(".floatingHeader").each(function() {
				$(this).appendTo($('#floatedHeaderContainer'));
			});
		}


		var setTableHeadWidths = function(originalTable, newTable) {
			var newTableHeadings = $(newTable).find("th");
			$(originalTable).find("th").each(function(index) {
				$(newTableHeadings[index]).width($(this).width());
			});
			originalTable.removeClass(".originalTableHead");
		}


		$(areaToPersist).each(function() {
			if($(".persist-header").length) {
				$(".persist-header").each(function() {
					if($(this).is("thead")) {
						cloneTableHead($(this, areaToPersist), "floatingHeader");
					} else {
						cloneRow($(this, areaToPersist), "floatingHeader");
					}
				});

				stackHeaders(areaToPersist);
			};
			if($(".persist-footer").length) cloneRow($(".persist-footer", areaToPersist), "floatingFooter");
		});






		$(window).scroll(function() {
			if(isScreenToBeFixed()) {
				updateTableHeaders(areaToPersist)
			} else {
				$(".floatingHeader", areaToPersist).invisible();
				$(".floatingFooter", areaToPersist).invisible();
			}
		});


		// keeps the width of the floating header/footer as the parent on window resize
		$( window ).resize(function() {
			$(".floatingHeader:visible").css("max-width", $(this).parent().width() );

			var floatingFooter = $(".floatingFooter");
			floatingFooter.css("max-width", floatingFooter.parent().width() );
		});


		// public methods
		this.initialize = function() {
			if(this.isScreenToBeFixed) {
				updateTableHeaders(areaToPersist);
				return this;
			}
		};

		var isScreenToBeFixed = function() {
		   return (options.minimumScreenHeightFix == 0 || $(window).height() > options.minimumScreenHeightFix);
		}


 		// method to fix the jumbo direction icon in place
		this.fixDirectionIcon = function() {
			var directionIcon = $('.direction-icon');
			var fixContainer = $('.fix-on-scroll-container');
			var persistAreaTop = $('.persist-header').height() + $('#primary-navigation').height();

			if(fixContainer.offset().top - $(window).scrollTop() < $('.persist-header').height() + $('#primary-navigation').height()) {
				directionIcon.css({ "top" : persistAreaTop, "position": "fixed", "max-width": directionIcon.width() });
			} else {
				directionIcon.css({"top": "auto", "position": "static", "max-width": directionIcon.width });
			}
		};

		// if the list of agents is shorter than the (viewport+fixed screen areas)
		// and we've scrolled past the top of the persist-area container, then fix it
		// (otherwise don't, because the user won't be able to see all of the items in the well)
		this.fixTargetList = function(listToFix) {
			var targetList = $(listToFix);
			var persistAreaTop = $('.floatingHeader:visible').height() + $('#primary-navigation').height();

			// width is set on fixing because it was jumping to the minimum width of the content
			if (targetList.height() < this.viewableArea() && ($(window).scrollTop() > $('.persist-area').offset().top)) {
				targetList.css({"top": persistAreaTop + 14, "position": "fixed", "width": targetList.parent().width()});
			} else {
				targetList.css({"top": "auto", "position": "relative", "width": "auto"});
			}
		}

		this.viewableArea = function() {
			return $(window).height() - ($('.persist-header:visible').height() + $('#primary-navigation').height() + $('.persist-footer').outerHeight());
		}


		return this.initialize();


	} // end fixHeaderFooter plugin


})(jQuery);