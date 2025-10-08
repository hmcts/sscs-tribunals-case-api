// Get the template HTML and remove it from the doument

/* global $ */
$(document).ready(function () {

    var previewNode = document.querySelector("#template");
    previewNode.id = "";
    var previewTemplate = previewNode.parentNode.innerHTML;
    previewNode.parentNode.removeChild(previewNode);

    var myDropzone = new Dropzone('#actions', { // Make the whole body a dropzone
        paramName: 'fileUpload',
        url: "/evidence-upload-interact", // Set the url
        thumbnailWidth: 80,
        thumbnailHeight: 80,
        parallelUploads: 20,
        previewTemplate: previewTemplate,
        autoQueue: true, // Make sure the files aren't queued until manually added
        previewsContainer: "#previews", // Define the container to display the previews
        clickable: ".fileinput-button" // Define the element that should be used as click trigger to select files.
    });

    //First check if there are any files
    $.ajax({
        type: 'GET',
        url: '/question-interacting-files',
        success: function(files) {
            if (files.length > 0) {
                files.forEach(function(file) {
                    var fileData = {
                        name: file.originalname,
                        size: file.size,
                        encoding: file.encoding,
                        mimetype: file.mimeType,
                        filename: file.filename,
                        path: file.path }
                   myDropzone.emit('addedfile', fileData);
                   myDropzone.emit('success', fileData);
                   myDropzone.files.push(fileData, fileData);
                });
            }
        }
    });

    var files = 0;
    // If we have no files, we need an empty message
    myDropzone.on("removedfile", function(file) {
        files--;
        if (files <= 0) {
            $('#back-to-tasks').attr('href', '/task-list');
            document.querySelector("#upload-documents").innerHTML = '<div class="c-uploads-item"><p class="c-uploads-empty">No files uploaded</p></div>';
        }
    });

    myDropzone.on("addedfile", function (file) {
        files++;
        $('#back-to-tasks').attr('href', '/task-list-upload?interactingCompletedOrDraft=draft');
        document.querySelector("#upload-documents").innerHTML = '';
    });

    myDropzone.on("success", function (file, response) {

        $(file.previewElement).find('#file-size').remove();
        $(file.previewElement).find('.preview-file').show();

        var previewLink = $(file.previewElement).find('.preview-file');
        var deleteLink = $(file.previewElement).find('.delete');
        $(previewLink).data('data-file', file);
        var dataAtt = response === undefined ? file : response;
        $(deleteLink).attr('data-file', JSON.stringify(dataAtt));

        /* Delete click event */

        $(deleteLink).click(function(e) {

            var fileData = $(e.currentTarget).attr('data-file');

            $.ajax({
                type: 'POST',
                contentType: "application/json",
                data: fileData,
                url: '/delete-question-interacting-files',
                success: function(files) {
                    $(file.previewElement).remove();
                    myDropzone.emit('removedfile', fileData);
                }
            });
        });

        /* Preview click event */

        $(previewLink).click(function(e) {

            var fileData = $(e.currentTarget).data('data-file');

            var reader  = new FileReader();
            reader.onload = function(e)  {
                var image = document.createElement("img");
                image.src = e.target.result;
                $('#imageModal .modal-content').append(image)
                $('#image-name').append('<p>' + fileData.name + '</p>');
                $('#imageModal').removeClass().addClass('active').show();
            };
            reader.readAsDataURL(fileData)
        });
    });

// Update the total progress bar
    myDropzone.on("totaluploadprogress", function (progress) {
        document.querySelector("#total-progress .progress-bar").style.width = progress + "%";
    });

    myDropzone.on("sending", function (file) {
        // Show the total progress bar when upload starts
        document.querySelector("#total-progress").style.opacity = "1";
        // And disable the start button
        // file.previewElement.querySelector(".start").setAttribute("disabled", "disabled");
    });

// Hide the total progress bar when nothing's uploading anymore
    myDropzone.on("queuecomplete", function (progress) {
        document.querySelector("#total-progress").style.opacity = "0";
    });

    /* close the preview */

    $('.close').click(function() {
        var modal = $('#imageModal');
        if (modal.hasClass('active')) {
            $('#modal-content').empty();
            $('#image-name').empty();
            modal.removeClass().hide();
        }
    });

});
