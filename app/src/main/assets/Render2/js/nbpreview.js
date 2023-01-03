(function () {
    var root = this;
    var $file_input = document.querySelector("input#file");
    var $holder = document.querySelector("#notebook-holder");
    var $controls = document.querySelector("#controls");
    var $error = document.querySelector("#error");
    var notebook = document.getElementById("main");


    var render_notebook = function (ipynb) {
        var notebook = root.notebook = nb.parse(ipynb);
        while ($holder.hasChildNodes()) {
            $holder.removeChild($holder.lastChild);
        }
        $holder.appendChild(notebook.render());
        Prism.highlightAll();
    };

    var load_file = function (file) {
        var reader = new FileReader();
        reader.onload = function (e) {
            var parsed = JSON.parse(this.result);
            render_notebook(parsed);
        };
        reader.readAsText(file);
    };

    $file_input.onchange = function (e) {
				if(this.files[0].name.endsWith('.ipynb')){
					load_file(this.files[0]);
					$controls.hidden=true;
					$error.textContent="";
					notebook.style.display = "block";
					$holder.style.fontSize = ".5em";
				}else{
					console.log("Invalid file");
					$error.textContent = "Please upload correct ipynb format file";
					$error.align = "center";
					$error.style.fontSize = "3.5em";
					$error.style.color = "red";
						 }

    };

}).call(this);
