import * as React from "react";
import * as ReactDOM from "react-dom";
import CodeMirror from "codemirror";
import Visualizer from "./visualizer";

declare const editor: CodeMirror.Editor;

ReactDOM.render(
    <Visualizer
        editor={editor}
    />,
    document.getElementById("root")
);