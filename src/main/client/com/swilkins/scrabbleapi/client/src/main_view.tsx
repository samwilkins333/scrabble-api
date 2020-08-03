
import * as React from "react";
import "./main_view.scss";
import { observer } from "mobx-react";
import { observable, runInAction, action } from "mobx";
import {Server} from "./utilities";

const dimensions = 15;

interface Cell { 
    active: boolean,
    ref: React.RefObject<HTMLInputElement>
}

@observer
export default class MainView extends React.Component {
    @observable.deep private cellArray: Cell[][] = [];
    @observable private candidates: string[] = [];
    private temporary: { x: number, y: number }[] = [];

    constructor(props: {}) {
        super(props);
        for (let y = 0; y < dimensions; y++) {
            const row: Cell[] = [];
            for (let x = 0; x < dimensions; x++) {
                row.push({ active: false, ref: React.createRef() });
            }
            this.cellArray.push(row);
        }
    }

    private get cells() {
        const collector: JSX.Element[] = [];
        for (let y = 0; y < dimensions; y++) {
            const row: JSX.Element[] = [];
            for (let x = 0; x < dimensions; x++) {
                const { active, ref } = this.cellArray[y][x];
                row.push(
                    <div className={"flex"}>
                        <input
                            ref={ref}
                            maxLength={1}
                            className={"cell"}
                            style={{
                                backgroundColor: active ? "red" : "white"
                            }}
                            onChange={action((e: React.ChangeEvent<HTMLInputElement>) => {
                                if (!(this.cellArray[y][x].active = /^[a-z]$/.test(e.target.value.toLowerCase()))) {
                                    e.target.value = "";
                                }
                            })}
                        />
                    </div>
                )
            }
            collector.push(<div className={"flex row"}>{...row}</div>)
        }
        return <div>{...collector}</div>;
    }

    private submitBoard = async () => {
        const board: { row: number, tiles: string  }[] = [];
        for (let y = 0; y < dimensions; y++) {
            const row: string[] = [];
            for (let x = 0; x < dimensions; x++) {
                row.push(this.cellArray[y][x].ref.current?.value || "-");
            }
            board.push({ row: y, tiles: row.join("") });
        }
        const rack = "a*febji";
        const { candidates } = await Server.Post("/api/generate", { board, rack });
        runInAction(() => this.candidates = candidates);
    };

    render() {
        return (
            <div className={"ssf w100 h100 flex col centering"}>
                <div className={"flex h50 w100 flexgrow1"}>
                    {this.cells}
                    <div className={"flex col scroll"}>
                        {this.candidates.map(candidate => (
                            <span
                                onClick={() => {
                                    for (const { x, y } of this.temporary) {
                                        const element = this.cellArray[y][x].ref.current;
                                        element && (element.value = "");
                                    }
                                    this.temporary = [];
                                    const matches = /\[([^\[\]]+)\]/.exec(candidate)!;
                                    const placementsRaw = matches[1];
                                    let placementMatch: RegExpExecArray | null;
                                    const regex = /([a-z\*\{\}]+)\((\d+), (\d+)\)/g;
                                    while ((placementMatch = regex.exec(placementsRaw))) {
                                        const x = Number(placementMatch[2]);
                                        const y = Number(placementMatch[3]);
                                        const element = this.cellArray[y][x].ref.current;
                                        if (element) {
                                            element.value = placementMatch[1].replace(/[\{*\}]+/g, "");
                                            this.temporary.push({ x, y, });
                                        }
                                    }
                                }}
                            >{candidate}</span>
                        ))}
                    </div>
                </div>
                <button
                    className={"pat20"}
                    onClick={this.submitBoard}
                >Submit</button>
            </div>
        );
    }

}