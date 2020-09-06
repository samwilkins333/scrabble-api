import * as React from "react";
import {EditorProps} from "./Visualizer";
import {StepRequestDepth} from "./StepRequestDepth";
import {observer} from "mobx-react";
import {action, observable} from "mobx";
import "./Visualizer.scss";

interface ControlsProps extends EditorProps {
    proceed(depth: StepRequestDepth): Promise<boolean>;
}

@observer
export default class Controls extends React.Component<ControlsProps> {
    @observable private disabled = false;

    render() {
        return (
            <div id="controls" className="flex centered bbb">
                {this.renderControlButton("Continue", StepRequestDepth.NONE)}
                {this.renderControlButton("Step Over", StepRequestDepth.STEP_OVER)}
                {this.renderControlButton("Step Into", StepRequestDepth.STEP_INTO)}
                {this.renderControlButton("Step Out", StepRequestDepth.STEP_OUT)}
            </div>
        );
    }

    @action
    private proceed = async (depth: StepRequestDepth) => {
        this.disabled = true;
        this.disabled = await this.props.proceed(depth);
    }

    private renderControlButton(label: string, depth: StepRequestDepth) {
        return (
            <button
                onClick={() => this.proceed(depth)}
                disabled={this.disabled}
            >{label}</button>
        );
    }

}