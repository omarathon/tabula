import $ from 'jquery';
import _ from 'lodash-es';

class MitCircsDetails {
  constructor(details) {
    this.details = details;
    this.init();
  }

  init() {
    const { details } = this;
    const $details = $(details);

    $details
      .find('.mitcircs-details__section.async')
      .each((i, el) => {
        const $asyncSection = $(el);
        const $content = $asyncSection.find('.content');
        $content.load($asyncSection.data('href'), (text, status, xhr) => {
          if (status === 'error') {
            $content.text(`Unable to load content: ${xhr.statusText || xhr.status || 'error'}`);
          } else {
            bind();
          }
        });
      });

    // Grow textarea when typing
    $details.on('keydown', 'textarea', (e) => {
      const $target = $(e.target);
      // For some reason carriage returns don't update the height until the methods returns,
      // so just wait a tick before checking the height
      setTimeout(() => {
        if ($target.get(0).scrollHeight > $target.get(0).offsetHeight && $target.prop('rows') < 10) {
          $target.prop('rows', $target.prop('rows') + 1);
        }
      }, 1);
    });

    // Add file number badge and update tooltip when files are selected
    $details.on('change', 'input[type=file]', (e) => {
      const $input = $(e.target);
      const $label = $input.closest('label');
      const $tooltip = $label.find('.use-tooltip');
      const originalText = $tooltip.attr('title') || $tooltip.data('original-title');
      const fileCount = ($input.prop('files') && $input.prop('files').length > 1) ? $input.prop('files').length : 1;
      const newText = (fileCount > 1) ? `${$input.prop('files').length} files selected` : $input.val().split('\\').pop();
      if (newText) {
        $tooltip.attr('data-original-title', newText);
        $label.append($('<span/>').addClass('badge').text(fileCount));
      } else {
        $tooltip.attr('data-original-title', originalText);
        $label.find('.badge').remove();
      }
    });

    $details.on('submit', (e) => {
      e.preventDefault();
      const $form = $(e.target);
      const $thread = $form.closest('.message-thread');
      const $content = $thread.closest('.mitcircs-details__section .content');
      $form.find(':input').prop('readonly', true);
      $form.find('button').prop('disabled', true);

      const formData = new FormData(e.target);
      $.ajax({
        url: $form.attr('action'),
        type: 'POST',
        data: formData,
        success: (data) => {
          $content.html(data);
          bind();
        },
        cache: false,
        contentType: false,
        processData: false
      });
    });

    // scroll to the end of the message container
    function messageScroll() {
      const $body = $details.find('.message-thread__body');
      $body.scrollTop($body.prop('scrollHeight'));
    }

    // Disable send button when the textarea is empty
    function checkAndUpdateSendButton(e) {
      const $thread = (e !== undefined) ? $(e.target).closest('.message-thread') : $details;
      const $textarea = $thread.find('.message-thread__footer__fields textarea');
      const $button = $thread.find('.message-thread__footer__fields button[type=submit]');
      if (_.trim($textarea.val()).length === 0) {
        $button.prop('disabled', true);
      } else {
        $button.prop('disabled', false);
      }
    }
    $details.on('keyup', checkAndUpdateSendButton);

    function bind() {
      checkAndUpdateSendButton();
      messageScroll();
      $('.use-tooltip').tooltip();
    }

  }
}

function init() {
  $('.mitcircs-details').each((i, el) => {
    $(el).data('tabula.mitCircsAsyncSection', new MitCircsDetails(el));
  });
}

$(() => init());
