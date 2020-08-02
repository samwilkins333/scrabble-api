
import * as React from "react";
import "./main_view.scss";
import { observer } from "mobx-react";
import {Server} from "./utilities";

const dimensions = 15;

@observer
export default class MainView extends React.Component {
    private cellArray: React.RefObject<HTMLInputElement>[][] = [];

    constructor(props: {}) {
        super(props);
        for (let y = 0; y < dimensions; y++) {
            const row: React.RefObject<HTMLInputElement>[] = [];
            for (let x = 0; x < dimensions; x++) {
                row.push(React.createRef());
            }
            this.cellArray.push(row);
        }
    }

    private get cells() {
        const collector: JSX.Element[] = [];
        for (let y = 0; y < dimensions; y++) {
            const row: JSX.Element[] = [];
            for (let x = 0; x < dimensions; x++) {
                row.push(
                    <div className={"flex"}>
                        <input
                            ref={this.cellArray[y][x]}
                            maxLength={1}
                            className={"cell"}
                        />
                    </div>
                )
            }
            collector.push(<div className={"flex row"}>{...row}</div>)
        }
        return collector;
    }

    private logBoard = async () => {
        const board: { row: number, tiles: string  }[] = [];
        for (let y = 0; y < dimensions; y++) {
            const row: string[] = [];
            for (let x = 0; x < dimensions; x++) {
                row.push(this.cellArray[y][x].current?.value || "-");
            }
            board.push({ row: y, tiles: row.join("") });
        }
        console.log((await Server.Post("/api/generate", {
            board,
            rack: "a*febji"
        })).candidates[0]);
    };

    render() {
        return (
            <div className={"main-container flex col centering"}>
                {this.cells}
                <button
                    className={"pat20"}
                    onClick={this.logBoard}
                >Submit</button>
            </div>
        );
    }

}