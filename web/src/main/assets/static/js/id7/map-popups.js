import _ from 'lodash-es';

import $ from 'jquery';

$(() => {
  const Config = {
    Defaults: {
      container: 'body',
      template: '<div class="popover wide"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>',
      html: true,
      content: _.template(['<iframe width="300" height="400" frameborder="0" src="<%- mapUrl %>"></iframe>']),
      placement: 'right',
      expandClickTarget: false,
    },
  };

  $.fn.mapPopups = function mapPopups(o) {
    this.each((i, popup) => {
      const $this = $(popup); const options = $.extend({}, Config.Defaults, o);
      $this.find('.map-location[data-lid]').add($this.filter('[data-lid]')).each((idx, el) => {
        const $el = $(el);
        if ($el.data('map-wired')) return;

        const mapUrl = `//campus.warwick.ac.uk/?lite=1&search=${encodeURIComponent($el.text())}&slid=${encodeURIComponent($el.data('lid'))}`;

        const $icon = $('<i />').addClass('fa').addClass('fa-map-marker');
        const $a = $('<a />').addClass('use-popover').attr('href', '#');
        const $srTitle = $('<span />').addClass('sr-only').text(`View ${$el.text()} on the campus map`);

        if (options.expandClickTarget) {
          $a.insertAfter($el);
          $el.remove()
            .appendTo($a)
            .append('&nbsp;')
            .append($icon)
            .append($srTitle);
        } else {
          $el
            .append('&nbsp;')
            .append(
              $a.append($icon)
                .append($srTitle),
            );
        }

        $a.tabulaPopover({
          trigger: 'click',
          container: options.container,
          template: options.template,
          html: options.html,
          content: options.content({ mapUrl }),
          placement: options.placement,
        });

        $el.data('map-wired', true);
      });
    });
  };

  $(() => {
    $('.map-location[data-lid]').mapPopups();

    // Look for popovers being shown
    $(document.body).on('shown.bs.popover shown.bs.modal', (e) => {
      $(e.currentTarget).mapPopups();
    });
  });
});
