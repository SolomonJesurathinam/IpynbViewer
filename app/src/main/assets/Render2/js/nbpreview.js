(function () {
    var root = this;
    var $holder = document.querySelector("#notebook-holder");
    var notebook = document.getElementById("main");

    // Extend nb for Plotly support
    if (root.nb) {
        root.nb.display["application/vnd.plotly.v1+json"] = function(plotlyData) {
            var chartDiv = document.createElement('div');
            chartDiv.classList.add('plotly-chart');
            chartDiv.style.width = '100%';
            chartDiv.style.maxWidth = '100%';
            
            if (window.Plotly) {
                setTimeout(function() {
                    try {
                        window.Plotly.newPlot(chartDiv, plotlyData.data, plotlyData.layout, plotlyData.config);
                    } catch (err) {
                        console.error('Plotly rendering error', err);
                    }
                }, 0);
            } else {
                chartDiv.textContent = "Plotly library not loaded";
                console.error("Plotly is not loaded on window");
            }
            return chartDiv;
        };
        
        // Insert at the front of display priority
        if (root.nb.display_priority.indexOf("application/vnd.plotly.v1+json") === -1) {
            root.nb.display_priority.unshift("application/vnd.plotly.v1+json");
        }
    }

    var render_notebook = function (ipynb) {
        var notebook = root.notebook = nb.parse(ipynb);
        while ($holder.hasChildNodes()) {
            $holder.removeChild($holder.lastChild);
        }
        $holder.appendChild(notebook.render());
        Prism.highlightAll();
    };

    root.largeLog = function(content, chunkSize = 4000) {
        for (let i = 0; i < content.length; i += chunkSize) {
            const chunk = content.substring(i, Math.min(i + chunkSize, content.length));
            console.log(chunk);
            }
        }


    let dataChunks = [];

    root.addDataChunk = function(chunk){
        dataChunks.push(chunk);
    }

    root.processData = function () {
            let fullData = dataChunks.join('');
            //largeLog(fullData); // Example: logging the full data
            dataChunks = []; // Clear the chunks array
            var parsed = JSON.parse(fullData);
            render_notebook(parsed);
            notebook.style.display = "block";
            $holder.style.fontSize = ".5em";
        }

}).call(this);
