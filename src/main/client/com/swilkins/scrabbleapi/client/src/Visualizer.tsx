import * as React from "react";
import {observer} from "mobx-react";
import CodeMirror from "codemirror";
import {observable, ObservableMap, runInAction} from "mobx";
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
    private dereferencedVariables = new ObservableMap<string, any>();

    render() {
        return (
            <>
                <Controls
                    {...this.props}
                    ref={this.controlsRef}
                    proceed={this.proceed}
                />
                <div className="flex centered flex-grow">
                    {this.renderValueBoxes()}
                </div>
            </>
        );
    }

    private proceed = async (depth: StepRequestDepth): Promise<boolean> => {
        const {editor} = this.props;

        this.currentLocation && this.setHighlightAtLine(this.currentLocation.lineNumber - 1, false);

        runInAction(() => this.dereferencedVariables.clear());

        const response = await Server.Post("/proceed", {
            depth,
            previousLocation: this.currentLocation
        });
        if (!response.updatedLocation) {
            editor.setValue("");
            setTimeout(() => alert("Execution completed!"), 100);
            return true;
        }

        const {updatedLocation, dereferencedVariables} = response;

        runInAction(() => {
            for (const variableName of Object.keys(dereferencedVariables)) {
                this.dereferencedVariables.set(variableName, dereferencedVariables[variableName]);
            }
        });

        console.log(this.dereferencedVariables);

        if (!this.currentLocation || this.currentLocation.className !== updatedLocation.className) {
            editor.setValue(response.contentsAsString);
        }
        const lineNumber = (this.currentLocation = updatedLocation).lineNumber - 1;
        this.scrollToLine(lineNumber);
        this.setHighlightAtLine(lineNumber, true);

        return false;
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

    private renderValueBoxes = () => {
        const namesSpanCollector: JSX.Element[] = [];
        const valuesSpanCollector: JSX.Element[] = [];
        this.dereferencedVariables.forEach((value, key) => {
            namesSpanCollector.push(<span>{key}</span>);
            if (Array.isArray(value)) {
                value = `[${value.join(", ")}]`;
            }
            valuesSpanCollector.push(<span>{value}</span>);
        });
        return (
            <>
                <div className="flex col flex-grow scrollable padded full_height_padded half_width_padded">
                    {...namesSpanCollector}
                </div>
                <div className="flex col flex-grow scrollable padded full_height_padded half_width_padded">
                    {...valuesSpanCollector}
                </div>
            </>
        )
    }

}