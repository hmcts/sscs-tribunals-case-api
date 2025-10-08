$(document).ready(function () {
  $('#provide-evidence-1').prop('checked', false);
  $('#provide-evidence-1').click(function () {
    $('#evidence-upload-reveal-container').toggle();
  });

  uploadEvidenceCounter = 0;
  displayRowsCount = 0;

  $('#uploadFileButton').change(function () {
    $('#upload-spinner').css('display', 'block');
    $('#uploadFileButton').hide();

    setTimeout(function() {
      uploadEvidenceCounter++;
      $('#uploadFileButton').show();
      $('#upload-spinner').css('display', 'none');
      $('#row_no_files').hide();
      evidenceNameArray = $('#file-upload-1').val().split('\\');
      evidenceName = evidenceNameArray[evidenceNameArray.length - 1];
      $('#evidence-list').prepend('<tr id="evidence_row' + uploadEvidenceCounter + '" class="govuk-table__row">\n' +
        '      <td class="govuk-table__cell">' + evidenceName + '</td>\n' +
        '      <td class="govuk-table__cell"><a id="deleteEvidence_' + uploadEvidenceCounter + '" href="/foo">Delete</a></td>\n' +
        '    </tr>');
      displayRowsCount++;
      function setupDeleteRow(currentUploadEvidenceCounter) {
        $('#deleteEvidence_' + currentUploadEvidenceCounter).click(function () {
          $('#evidence_row' + currentUploadEvidenceCounter).hide();
          displayRowsCount--;
          if (displayRowsCount === 0) {
            $('#row_no_files').show();
          }
          return false;
        })
      }
      setupDeleteRow(uploadEvidenceCounter);
    }, 1500);
  });
});