/* eslint-env browser */
import $ from 'jquery';

$(() => {
  $('.fix-area').fixHeaderFooter();
  $('.table-sortable').sortableTable({
    sortLocaleCompare: true,
    textAttribute: 'data-sortby',
  });

  // Auto grade generator
  $('.auto-grade[data-mark][data-generate-url]').each((i, el) => {
    const $input = $(el);
    const generateUrl = $input.data('generate-url');
    const $markInput = $input.closest('form').find(`[name="${$input.data('mark')}"]`);
    const $select = $input.next('select');

    if ($input.length && $markInput.length && $select.length) {
      let currentRequest;

      const doRequest = () => {
        if (currentRequest !== undefined) {
          currentRequest.abort();
        }

        const data = {
          mark: $markInput.val(),
          resitAttempt: $input.data('resit-attempt') || undefined,
        };

        if ($select.is(':visible')) {
          data.existing = $select.val();
        } else if ($input.val().length > 0) {
          data.existing = $input.val();
        }

        currentRequest = $.ajax(generateUrl, {
          type: 'POST',
          data,
          success: (html) => {
            $select.html(html);
            if ($select.find('option').length > 1) {
              $input.hide().prop('disabled', true);
              $select.prop('disabled', false).show();
            } else {
              $input.show().prop('disabled', false);
              $select.prop('disabled', true).hide();
            }
          },
          error: (xhr, errorText) => {
            if (errorText !== 'abort') {
              $input.show().prop('disabled', false);
            }
          },
        });
      };

      $markInput.on('change input', doRequest);
      doRequest();
    }
  });

  // Treat enter as tab in .marks-form
  $('form.marks-form').each((i, form) => {
    const $form = $(form);

    // All visible inputs that:
    // - Aren't buttons (including submit inputs)
    // - Aren't textareas (so we can still add newlines)
    $form.on('keydown', ':input:visible:not(:button):not(input[type="submit"]):not(textarea)', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();

        // Tabbable inputs are all form inputs, buttons and links that are visible and aren't
        // excluded from tabindex
        const $tabbable = $form.find(':input, :button, a').filter(':visible:not([tabindex="-1"])');
        const currentIndex = $tabbable.index(document.activeElement);

        $tabbable.eq(currentIndex + 1).focus();
      }
    });

    $form.on('click', '[data-toggle="copy-values"][data-target]', (e) => {
      const $button = $(e.target);
      const $target = $($button.data('target'), $form);

      $target.find('.mma-suggestion-field').each((j, data) => {
        const $data = $(data);
        const $dataTarget = $(document.getElementById($data.data('target')));
        const value = $data.val();

        if ($dataTarget.is('select')) {
          const $option = $dataTarget.find('option').filter((k, option) => $(option).val() === value);
          if ($option.length === 0) {
            $dataTarget.append($('<option />').attr('value', value).text(value));
          }

          $dataTarget.val(value);
        } else {
          $dataTarget.val(value);
        }
      });
    });
  });
});
