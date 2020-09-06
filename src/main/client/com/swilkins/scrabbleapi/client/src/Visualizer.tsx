import * as React from "react";
import {observer} from "mobx-react";
import CodeMirror from "codemirror";
import {observable, runInAction} from "mobx";
import Controls from "./Controls";
import {StepRequestDepth} from "./StepRequestDepth";
import {Server} from "./Utilities";
import "./Visualizer.scss";

export interface EditorProps {
    editor: CodeMirror.Editor;
}

@observer
export default class Visualizer extends React.Component<EditorProps> {
    private controlsRef = React.createRef<Controls>();
    @observable private currentLocation: { className: string, lineNumber: number } | null = null;
    @observable private variableNames: string[] = [];
    @observable private variableValues: string[] = [];

    render() {
        return (
            <>
                <Controls
                    {...this.props}
                    ref={this.controlsRef}
                    proceed={this.proceed}
                />
                <div className="flex centered flex-grow">
                    {this.renderValueBox(this.variableNames)}
                    {this.renderValueBox(this.variableValues)}
                </div>
            </>
        );
    }

    private proceed = async (depth: StepRequestDepth): Promise<boolean> => {
        const {editor} = this.props;

        this.currentLocation && this.setHighlightAtLine(this.currentLocation.lineNumber - 1, false);

        runInAction(() => {
            this.variableNames = [];
            this.variableValues = [];
        });

        const response = await Server.Post("/proceed", {
            depth,
            previousLocation: this.currentLocation
        });
        if (!response.updatedLocation) {
            editor.setValue("");
            setTimeout(() => alert("Execution completed!"), 100);
            return false;
        }

        const {updatedLocation, dereferencedVariables} = response;

        runInAction(() => {
            this.variableNames = Object.keys(dereferencedVariables);
            this.variableValues = Object.values(dereferencedVariables);
        });

        if (!this.currentLocation || this.currentLocation.className !== updatedLocation.className) {
            editor.setValue(response.contentsAsString);
        }
        const lineNumber = (this.currentLocation = updatedLocation).lineNumber - 1;
        this.scrollToLine(lineNumber);
        this.setHighlightAtLine(lineNumber, true);

        return true;
    }

    private scrollToLine = (i: number) => {
        const {editor} = this.props;
        const t = editor.charCoords({line: i, ch: 0}, "local").top;
        const middleHeight = editor.getScrollerElement().offsetHeight / 2;
        editor.scrollTo(null, t - middleHeight - 5);
    }

    private setHighlightAtLine = (i: number, flag: boolean) => {
        const {editor} = this.props;
        const lineHandle = editor.getLineHandle(i);
        if (flag) {
            editor.addLineClass(lineHandle, "background", "highlight");
        } else {
            editor.removeLineClass(lineHandle, "background", "highlight");
        }
    }

    private renderValueBox = (contents: string[]) => {
        return <div
            className="flex col flex-grow scrollable padded valueBox"
        >{contents.map(item => <span>{item}</span>)}</div>
    }

}