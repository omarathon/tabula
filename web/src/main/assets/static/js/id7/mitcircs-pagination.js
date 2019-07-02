/* eslint-env browser */
/* eslint-disable no-use-before-define */
import $ from 'jquery';

class MitCircsPagination {
  constructor(container) {
    this.container = container;
    this.init();
  }

  init() {
    const { container } = this;
    const $container = $(container);

    const $previous = $('[data-pagination="previous"]', $container);
    const $next = $('[data-pagination="next"]', $container);

    if ($previous.length > 0 || $next.length > 0) {
      $(document).on('keydown.tabula.pagination', (ev) => {
        // Ignore events that target an input
        if ($(ev.target).is(':input')) return;

        if ($previous.length > 0 && ev.key && ev.key.toLowerCase() === 'k') {
          $previous[0].click();
          ev.preventDefault();
        } else if ($next.length > 0 && ev.key && ev.key.toLowerCase() === 'j') {
          $next[0].click();
          ev.preventDefault();
        }
      });

      // When the next or previous links are clicked, remove the keydown handler above
      $previous.add($next).on('click', () => $(document).off('keydown.tabula.pagination'));
    }
  }
}

function init() {
  $('.mitcircs-pagination').each((i, el) => {
    $(el).data('tabula.mitCircsPagination', new MitCircsPagination(el));
  });
}

$(() => init());
