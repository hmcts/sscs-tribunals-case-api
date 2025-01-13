(function (root) {
  
  
// This is NOT production ready! I have prepared something for Steve Wise (SSCS)
// This requires a lot of work for JS and non-JS as well as styling and testing


  "use strict";


  // check if element exists
  var element = document.querySelector(".dropzone");
  if (!element) return;


  Dropzone.options.uploadWidget = {

    paramName: "file",
    maxFilesize: 10, // MB
    maxFiles: 10,
    dictDefaultMessage: "Drag and drop files here or click to choose a file",
    headers: {
      "x-csrf-token": document.querySelectorAll("meta[name=csrf-token]")[0].getAttributeNode("content").value,
    },
    acceptedFiles: "image/*, application/pdf",

    // Init
    init: function() {


      var files = 0;


      // If we have no files, we need an empty message
      this.on("removedfile", function(file) {

          files--;

          if (files <= 0) {
            document.querySelector("#upload-documents").innerHTML = '<div class="c-uploads-item"><p class="c-uploads-empty">No files uploaded</p></div>';
          }

        }),


        this.on("addedfile", function(file) {

          files++;

          // If we have 1 file then we no longer need an empty message
          if (files === 1) {
            document.querySelector("#upload-documents").innerHTML = '';
          }


          // Create the remove button
          var removeButton = Dropzone.createElement('<div class="c-uploads-item"><span class="c-uploads-item__filename">' + file.name + '</span><span class="c-uploads-item__action"><button class="c-uploads-button" aria-controls="upload-documents">Remove</button></span></div>');



          // Get the element I want to add new elements to
          var target = document.querySelector("#upload-documents");


          // Capture the Dropzone instance as closure
          var _this = this;


          // Listen to the click event
          removeButton.addEventListener("click", function(e) {

            // Make sure the button click doesn't submit the form
            e.preventDefault();
            e.stopPropagation();

            // Remove the file preview
            _this.removeFile(file);

            // Remove item
            this.remove();

          });

          target.appendChild(removeButton);

        });

    }

  };
  

})(this);