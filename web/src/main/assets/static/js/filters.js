/* eslint-env browser */
import $ from 'jquery';

const { AjaxPopup } = window;

$.fn.enableFilters = function filters(o) {
  const options = o || {};
  const resultSelector = options.resultSelector || '.filter-results';

  this.each(function init() {
    const $this = $(this);
    const $form = $this.find('form');
    const $results = $(resultSelector);
    const $clearAll = $this.find('.clear-all-filters');

    function toggleClearAll() {
      const checkboxes = $this.find('input:checked').length;
      const dateFields = $this.find('.date-picker').filter((i, el) => el.value.length > 0).length;
      $clearAll.prop('disabled', (checkboxes + dateFields === 0));
    }

    function updateFilterText($checkbox) {
      const $filter = $checkbox.closest('.filter');
      const shortValues = $filter.find('input:checked').map(function toShortValue() {
        return $(this).data('short-value');
      }).get();
      const $fsv = $filter.find('.filter-short-values');
      if (shortValues.length) {
        $filter.removeClass('empty');
        $fsv.html($fsv.data('prefix') + shortValues.join(', '));
      } else {
        $filter.addClass('empty');
        $fsv.html($fsv.data('placeholder'));
      }
    }

    // hides any invalid options in related filters
    function updateRelatedFilters($checkbox) {
      const related = $checkbox.data('related-filter');
      if (related) {
        const name = $checkbox.attr('name');
        const values = $(`input[name=${name}]:checked`).map(function toVal() {
          return $(this).val();
        });
        const $relatedFilter = $(`#${related}-filter`);
        if (values.length > 0) {
          // hide all filters
          $relatedFilter.find('li:not(:has(>.module-search))').hide();
          // show filters that match the value of this filter
          $.each(values, (index, value) => {
            $relatedFilter.find(`input[data-related-value="${value}"]`).closest('li').show();
          });
        } else {
          $relatedFilter.find('li').show();
        }
      }
    }

    function doRequest(eventDriven) {
      // update the url with the new filter values
      const serialized = $form.find(':input').filter((index, element) => $(element).val() !== '').serialize();
      const url = `${$form.attr('action')}?${serialized}`;
      if (eventDriven && typeof window.history.pushState !== 'undefined') {
        window.history.pushState(null, document.title, url);
      }

      // abort any currently running requests
      if ($form.data('request')) {
        $form.data('request').abort();
        $form.data('request', null);
      }

      // grey out results while loading
      $results.addClass('loading').html('<i class="fal fa-spinner fa-spin"></i> Loading&hellip;');

      $form.data('request', $.get(`${url}&_ts=${new Date().getTime()}`, (data) => {
        $(document).trigger('tabula.beforeFilterResultsChanged');
        $results.html(data);
        $form.data('request', null);
        $results.removeClass('loading');

        $('.use-wide-popover').tabulaPopover({
          trigger: 'click',
          container: 'body',
          template: '<div class="popover wide"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>',
        });

        $('.use-tooltip').tooltip({ sanitize: false });
        AjaxPopup.wireAjaxPopupLinks($('body'));

        // callback for hooking in local changes to results
        $(document).trigger('tabula.filterResultsChanged');
      }));
      toggleClearAll();
    }

    // add a clear list when at least one option is checked
    function prependClearLink($list) {
      if (!$list.find('input:checked').length) {
        $list.find('.clear-this-filter').remove();
      } else if (!$list.find('.clear-this-filter').length) {
        $list.find('> ul').prepend(
          $('<li />')
            .addClass('clear-this-filter')
            .append(
              $('<button />').attr('type', 'button')
                .addClass('btn btn-link')
                .html('<i class="icon-ban-circle fa fa-ban"></i> Clear selected items')
                .on('click', () => {
                  $list.find('input:checked').each(function updateFilters() {
                    const $checkbox = $(this);
                    $checkbox.prop('checked', false);
                    updateFilterText($checkbox);
                    updateRelatedFilters($checkbox);
                  });
                  doRequest(true);
                }),
            )
            .append($('<hr />')),
        );
      }
    }

    function updateFilterFromPicker($picker, name, value, sv) {
      if (value === undefined || value.length === 0) {
        return;
      }

      const shortValue = sv || value;
      const $ul = $picker.closest('ul');
      const $li = $ul.find(`input[value="${value}"], input[value="Module(${value})"]`).closest('li');
      let $checkbox;
      if ($li.length) {
        $checkbox = $li.find('input').prop('checked', true);
        if ($ul.find('li.check-list-item:first').find('input').val() !== value) {
          $li.insertBefore($ul.find('li.check-list-item:first'));
        }
      } else {
        $checkbox = $('<input/>').attr({
          type: 'checkbox',
          name,
          value,
          checked: true,
        }).data('short-value', shortValue);

        $('<li/>').addClass('check-list-item').append(
          $('<label/>').addClass('checkbox').append($checkbox).append(` ${$picker.val()}`),
        ).insertBefore($ul.find('li.check-list-item:first'));
      }

      doRequest(true);
      updateFilterText($checkbox);
    }

    // bind event handlers
    $this.on('change', (e) => {
      const $checkbox = $(e.target);
      doRequest(true);
      updateFilterText($checkbox);
      updateRelatedFilters($checkbox);
    });

    if ($this.data('lazy')) {
      doRequest(false);
    }

    $this.find('.module-picker').on('change', function changeHandler() {
      const $picker = $(this);
      const name = $picker.data('name') || 'modules';

      if ($picker.data('modulecode') === undefined || $picker.data('modulecode').length === 0) {
        return;
      }

      const value = $picker.data('wrap') ? `Module(${$picker.data('modulecode')})` : $picker.data('modulecode');

      updateFilterFromPicker($picker, name, value, $picker.data('modulecode').toUpperCase());

      $picker.data('modulecode', '').val('');
    });

    $clearAll.on('click', () => {
      $this.find('input:checked').each(function updateFilters() {
        const $checkbox = $(this);
        $checkbox.prop('checked', false);
        updateFilterText($checkbox);
        updateRelatedFilters($checkbox);
      });
      $this.find('input[type="text"]').val('');
      doRequest(true);
    });
    toggleClearAll();

    // Re-order elements inside the dropdown when opened
    $('.filter-list', $this).closest('.btn-group').find('.dropdown-toggle').on('click.dropdown.data-api', function clickHandler() {
      const $dropdown = $(this);
      if (!$dropdown.closest('.btn-group').hasClass('open')) {
        // Re-order before it's opened!
        const $list = $dropdown.closest('.btn-group').find('.filter-list');
        const items = $list.find('li.check-list-item').get();

        items.sort((a, b) => {
          const aChecked = $(a).find('input').is(':checked');
          const bChecked = $(b).find('input').is(':checked');

          if (aChecked && !bChecked) return -1;
          if (!aChecked && bChecked) return 1;
          return $(a).data('natural-sort') - $(b).data('natural-sort');
        });

        $.each(items, (item, el) => {
          $list.find('> ul').append(el);
        });

        prependClearLink($list);
      }
    });

    $('input:checked').each((index, checkbox) => {
      updateFilterText($(checkbox));
    });

    $('input[data-related-filter]:checked').each((index, checkbox) => {
      updateRelatedFilters($(checkbox));
    });
  });
};

$(() => {
  $('.filters').enableFilters();
});
