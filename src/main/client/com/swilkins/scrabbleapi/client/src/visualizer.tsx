import * as React from "react";
import CodeMirror from "codemirror";

interface VisualizerProps {
    editor: CodeMirror.Editor;
}

export default class Visualizer extends React.Component<VisualizerProps> {

    private setContents = () => {
        this.props.editor.setValue("System.out.println(\"Asynchronous, baby!\");");
    }

    componentDidMount() {
        setTimeout(this.setContents, 5000);
    }

    render() {
        return (
            <div>
                Hello there!
            </div>
        );
    }

}