(function($){ "use strict";

/* fixHeaderFooter plugin
	 *
	 * apply this to a container div
	 * which should contain one or more divs called .persist-header and/or a single .persist-footer
	 * it'll fix the headers (in order) at the top when scrolled
	 * and it'll fix the footer at the bottom.
	 * (CSS in main.less)
	 *
	 */
    $.fn.fixHeaderFooter = function() {
        var selHeader = '.persist-header';
        var selClonedHeader = '.cloned-header';
        var selHeaderShadow = '.header-shadow';
        var selFooter = '.persist-footer';
        var selFooterShadow = '.footer-shadow';
        var $container = this;
        var $pnw = $('#primary-navigation-wrapper');
        var theadProperties = [
            'width',
            'border-top-left-radius', 'border-top-right-radius',
            'border-top-color', 'border-right-color', 'border-bottom-color', 'border-left-color',
            'border-top-width', 'border-right-width', 'border-bottom-width', 'border-left-width',
            'border-top-style', 'border-right-style', 'border-bottom-style', 'border-left-style'
        ];
        var isSmallscreen = false;

        var $headers = $container.find(selHeader);

        // use .each() as we allow more than one header to fix and we need a $(this) reference
        $headers.each(function(idx) {
            $(this).scrollToFixed({
                zIndex: 10,
                marginTop: function() {
                    // calculate offset for primary nav, as it's fixed outside this plugin
                    var navOffset = $pnw.filter(':visible').length ? $pnw.outerHeight() : 0;

                    // calculate based on previous headers
                    var fixedHeight = navOffset;
                    var $fixedSoFar = $(selHeader + '.scroll-to-fixed-fixed').not($(this));
                    $fixedSoFar.each(function() {
                        fixedHeight += $(this).outerHeight();
                    });
                    return fixedHeight;
                },
                preFixed: function() {
                    if ($(this).is('thead')) {
                        var $this = $(this);

                        // before we let the generic plugin fix the <thead> position, pre-save <table> properties which lose inheritance on fix
                        var $table = $this.parent('table');
                        for (var i in theadProperties) $this.data(theadProperties[i], $table.css(theadProperties[i]));
                        $this.data('width', $table.width());

                        if (!$(selClonedHeader).length) {
                            // clone <thead> to maintain column widths in the <table> after the original is taken out of flow
                            var $ph = $this
                                .clone()
                                .addClass(selClonedHeader.substring(1))
                                .removeClass(selHeader.substring(1))
                                .hide();
                            $this.after($ph);
                        }
                    }
                },
                fixed: function() {
                    var $this = $(this);

                    // add breathing room to first header
                    if ($this.is($headers.filter(':first'))) {
                        $this.css({
                            'padding-top': '8px',
                            'transition': 'padding-top 750ms'
                        });
                    };

                    // on fixing, if this is the last header to stack, add a shadow if absent
                    if ($this.is($headers.filter(':last')) && !$this.children(':last-child').is(selHeaderShadow)) $this.append('<hr class="' + selHeaderShadow.substring(1) + '">');

                    if ($this.is('thead')) {
                        // this is awful, but I've got no better after best part of day hacking/googling
                        var firefoxFudgeFactor = 1/$this.find('th,td').length;

                        // show the cloned <thead> (behind the original, fixed version)
                        var $ph = $(selClonedHeader);
                        $ph.css('display', 'table-header-group');

                        // apply other presaved properties to the original <thead>
                        for (var i in theadProperties) $this.css(theadProperties[i], $this.data(theadProperties[i]));
                        $this.width($this.data('width'));

                        // correct original <th> widths
                        $ph.find('th').each(function (i, th) {
                            var w = Math.ceil($(th).width()) + firefoxFudgeFactor;
                            $(th).width(w);
                            $this.find('th').eq(i).width(w);
                        });
                    }
                },
                preUnfixed: function() {
                    if ($(this).is($headers.filter(':first'))) {
                        $(this).css({
                            'padding-top': 'inherit',
                            'transition': 'none'
                        });
                    };
                },
                unfixed: function() {
                    var $this = $(this);

                    // remove shadows
                    if ($this.is($headers.filter(':last'))) $headers.children(':last-child').remove(selHeaderShadow);

                    // restore <thead> properties, remove cloned header
                    if ($this.is('thead')) {
                        for (var i in theadProperties) $(this).css(theadProperties[i], 'inherit');
                        $this.width('');
                        $(selClonedHeader).hide();
                    }
                }
            })
        });

        var $f = $container.find(selFooter);
        var $fm = $('<div class="footer-marker" />');
        $f.before($fm);
        $f.scrollToFixed({
            zIndex: 10,
            bottom: 0,
            limit: function() {
                return $fm.offset().top;
            },
            fixed: function() {
                if (!$f.children(':first-child').is(selFooterShadow)) $f.prepend('<hr class="' + selFooterShadow.substring(1) + '">');
            },
            unfixed: function() {
                $f.children(':first-child').remove(selFooterShadow);
            }
        });

        $('body').on('smallscreen', function(e) {
            isSmallscreen = !!$('body.is-smallscreen').length;
            var $sel = $headers.add($f);
            $sel.trigger(isSmallscreen ? 'disable.ScrollToFixed' : 'enable.ScrollToFixed');
        });

        // rest of the plugin contains helper method, primarily for drag-and-drop allocation
        this.viewableArea = function() {
            return $(window).height() - ($('.persist-header:visible').height() + $('#primary-navigation-wrapper').outerHeight() + $('.persist-footer').outerHeight());
        }

        // fix the jumbo direction icon in place
        this.fixDirectionIcon = function() {
            var $directionIcon = $('.direction-icon');
            var fixContainer = $('.fix-on-scroll-container');
            var persistAreaTop = $('.persist-header').height() + $('#primary-navigation-wrapper').outerHeight();

            if(!isSmallscreen && fixContainer.offset().top - $(window).scrollTop() < persistAreaTop) {
                $directionIcon.css({ "top" : persistAreaTop, "position": "fixed", "max-width": $directionIcon.width() });
            } else {
                $directionIcon.css({"top": "auto", "position": "static", "max-width": $directionIcon.width });
            }
        };

        // if the list of agents is shorter than the (viewport+fixed screen areas)
        // and we've scrolled past the top of the persist-area container, then fix it
        // (otherwise don't, because the user won't be able to see all of the items in the well)
        this.fixTargetList = function(listToFix) {
            var targetList = $(listToFix);
            var persistAreaTop = $('.persist-header').height() + $('#primary-navigation-wrapper').outerHeight();

            // width is set on fixing because it was jumping to the minimum width of the content
            if (!isSmallscreen && targetList.height() < this.viewableArea() && ($(window).scrollTop() > $('.persist-area').offset().top)) {
                targetList.css({"top": persistAreaTop + 14, "position": "fixed", "width": targetList.parent().width()});
            } else {
                targetList.css({"top": "auto", "position": "relative", "width": "auto"});
            }
        }

        return this;
    } // end fixHeaderFooter plugin
})(jQuery);