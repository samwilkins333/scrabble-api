let currentLocation = null;

const editorDiv = document.getElementById("editor");
editor = CodeMirror(editorDiv, {
    lineNumbers: true,
    mode: "text/x-java",
    matchBrackets: true,
    readOnly: "nocursor"
});

editor.setSize("100%", "100%");

async function proceed(depth) {
    currentLocation && setHighlightAtLine(currentLocation.lineNumber - 1, false);
    clear("variableNames");
    clear("variableValues");

    const body = {
        depth,
        previousLocation: currentLocation
    };
    const response = await (await fetch("/proceed", {
        method: "POST",
        body: JSON.stringify(body),
        headers: {"Content-Type": "application/json"}
    })).json();
    if (!response.updatedLocation) {
        for (const button of document.getElementsByTagName("button")) {
            button.disabled = true;
        }
        editor.setValue("");
        setTimeout(() => alert("Execution completed!"), 100);
        return;
    }

    const {updatedLocation, dereferencedVariables} = response;

    populate("variableNames", Object.keys(dereferencedVariables));
    populate("variableValues", Object.values(dereferencedVariables));

    if (!currentLocation || currentLocation.className !== updatedLocation.className) {
        editor.setValue(response.contentsAsString);
    }
    const lineNumber = (currentLocation = updatedLocation).lineNumber - 1;
    scrollToLine(lineNumber);
    setHighlightAtLine(lineNumber, true);
}

function populate(id, stringList) {
    const variableNames = document.getElementById(id);
    stringList.forEach(key => {
        const span = document.createElement("span");
        span.textContent = key;
        variableNames.appendChild(span);
    });
}

function clear(id) {
    const node = document.getElementById(id);
    while (node.firstChild) {
        node.removeChild(node.lastChild);
    }
}

function scrollToLine(i) {
    const t = editor.charCoords({line: i, ch: 0}, "local").top;
    const middleHeight = editor.getScrollerElement().offsetHeight / 2;
    editor.scrollTo(null, t - middleHeight - 5);
}

function setHighlightAtLine(i, flag) {
    const lineHandle = editor.getLineHandle(i);
    if (flag) {
        editor.addLineClass(lineHandle, "background", "highlight");
    } else {
        editor.removeLineClass(lineHandle, "background", "highlight");
    }
}