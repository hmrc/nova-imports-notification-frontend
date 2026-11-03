(function () {
  'use strict';

  var refreshLink = document.getElementById('refreshLink');
  if (refreshLink) {
    refreshLink.remove();
  }

  var config = document.getElementById('vinUploadStatusConfig');
  if (!config) {
    return;
  }

  var statusUrl = config.getAttribute('data-status-url');
  var refreshIntervalMs = parseInt(config.getAttribute('data-refresh-interval-ms'), 10);
  var maxPollDurationMs = parseInt(config.getAttribute('data-max-poll-ms'), 10);
  var timeoutUrl = config.getAttribute('data-timeout-url');
  var isFinal = config.getAttribute('data-is-final') === 'true';

  var pollStartedAt = Date.now();

  document.addEventListener('DOMContentLoaded', function () {
    if (!isFinal) {
      window.setTimeout(checkUploadStatus, refreshIntervalMs);
    }
  });

  function wireContinueButton() {
    var continueButton = document.getElementById('continueButton');
    if (!continueButton) {
      return;
    }
    var href = continueButton.getAttribute('data-continue-href');
    if (href) {
      continueButton.addEventListener('click', function () {
        window.location.href = href;
      });
    }
  }

  function scheduleNextCheck() {
    if (Date.now() - pollStartedAt >= maxPollDurationMs) {
      window.location.href = timeoutUrl;
      return;
    }
    window.setTimeout(checkUploadStatus, refreshIntervalMs);
  }

  function checkUploadStatus() {
    fetch(statusUrl)
      .then(function (response) {
        return response.json();
      })
      .then(function (data) {
        if (data.redirectUrl) {
          window.location.href = data.redirectUrl;
          return;
        }

        var tag = document.getElementById('upload-status-tag');
        var continueButton = document.getElementById('continueButton');
        var actionLink = document.getElementById('upload-action-link');
        var fileNameCell = document.querySelector('.upload-file-name');

        if (tag && data.statusText) {
          tag.textContent = data.statusText;
        }

        if (fileNameCell && data.fileName) {
          fileNameCell.textContent = data.fileName;
        }

        if (data.isFinal) {
          if (tag) {
            tag.classList.remove('govuk-tag--yellow');
            tag.classList.add('govuk-tag--green');
          }
          if (continueButton) {
            continueButton.removeAttribute('disabled');
            continueButton.removeAttribute('aria-disabled');
            if (data.continueUrl) {
              continueButton.setAttribute('data-continue-href', data.continueUrl);
            }
            wireContinueButton();
          }
          if (actionLink) {
            actionLink.href = actionLink.getAttribute('data-remove-href');
            for (var i = 0; i < actionLink.childNodes.length; i++) {
              if (actionLink.childNodes[i].nodeType === Node.TEXT_NODE) {
                actionLink.childNodes[i].textContent = actionLink.getAttribute('data-remove-text');
                break;
              }
            }
          }
        } else {
          scheduleNextCheck();
        }
      })
      .catch(function () {
        scheduleNextCheck();
      });
  }
})();
