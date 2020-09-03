import * as React from "react";
import "./main_view.scss";
import {observer} from "mobx-react";
import {action, observable, runInAction} from "mobx";
import {Server} from "./utilities";

const dimensions = 15;

interface Cell {
    active: boolean,
    ref: React.RefObject<HTMLInputElement>
}

interface Tile {
    letter: string;
    value: number;
    resolvedLetter: string;
    letterProxy?: string;
}

interface TilePlacement {
    x: number;
    y: number;
    tile: Tile
}

const pageSize = 10;

@observer
export default class MainView extends React.Component {
    @observable.deep private cellArray: Cell[][] = [];
    @observable private candidates: TilePlacement[][][] = [];
    @observable private pageIndex = 0;
    @observable private pageCount = 0;
    private currentSelection: TilePlacement[] = [];

    constructor(props: {}) {
        super(props);
        for (let y = 0; y < dimensions; y++) {
            const row: Cell[] = [];
            for (let x = 0; x < dimensions; x++) {
                row.push({active: false, ref: React.createRef()});
            }
            this.cellArray.push(row);
        }
    }

    private get cells() {
        const collector: JSX.Element[] = [];
        for (let y = 0; y < dimensions; y++) {
            const row: JSX.Element[] = [];
            for (let x = 0; x < dimensions; x++) {
                const {active, ref} = this.cellArray[y][x];
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
        this.currentSelection = [];
        runInAction(() => this.pageCount = this.pageIndex = 0);
        const board: { row: number, tiles: string }[] = [];
        for (let y = 0; y < dimensions; y++) {
            const row: string[] = [];
            for (let x = 0; x < dimensions; x++) {
                row.push(this.cellArray[y][x].ref.current?.value || "-");
            }
            board.push({row: y, tiles: row.join("")});
        }
        const rack = "a*febji";
        const options = {
            raw: true,
            pageSize
        };
        const response = await Server.Post("/api/generate", {board, rack, options});
        runInAction(() => {
            this.candidates = response.candidates;
            this.pageCount = response.pageCount;
        });
    };

    @action
    private setValueAt = (x: number, y: number, value: string) => {
        const cell = this.cellArray[y][x];
        cell.active = value !== "";
        const {current} = cell.ref;
        current && (current.value = value);
    };

    private display = (candidate: TilePlacement[]) => {
        const word = candidate.map(({tile}) => {
            return tile.letterProxy ? tile.resolvedLetter.toUpperCase() : tile.resolvedLetter;
        }).join("");
        const location = candidate.map(({x, y}) => {
            return `(${x},${y})`
        }).join(" ");
        return `${word} [${location}]`;
    };

    private clear = () => {
        for (const {x, y} of this.currentSelection) {
            this.setValueAt(x, y, "");
        }
    };

    private get currentPage() {
        return this.pageIndex >= this.candidates.length ? [] : this.candidates[this.pageIndex]
    }

    private proceed = async () => {
        console.log(await Server.Post("/proceed", {depth: 1}));
    }

    render() {
        return (
            <div className={"ssf w100 h100 flex col centering"}>
                <div className={"flex h50 w100 scroll flexgrow1"}>
                    {this.cells}
                    <div className={"flex col"}>
                        {this.currentPage.map(candidate => (
                            <span
                                onClick={() => {
                                    this.clear();
                                    for (const {x, y, tile: {resolvedLetter}} of this.currentSelection = candidate) {
                                        this.setValueAt(x, y, resolvedLetter);
                                    }
                                }}
                            >{this.display(candidate)}</span>
                        ))}
                    </div>
                </div>
                <div className={"flex col ma-v20"}>
                    <button onClick={this.proceed}>Submit</button>
                    <div className={"flex"}>
                        <button onClick={action(() => {
                            this.clear();
                            const previous = this.pageIndex - 1;
                            this.pageIndex = previous < 0 ? this.pageCount - 1 : previous;
                        })}>Previous Page
                        </button>
                        <span>{this.pageIndex + 1} / {this.pageCount}</span>
                        <button onClick={action(() => {
                            this.clear();
                            const next = this.pageIndex + 1;
                            this.pageIndex = next == this.pageCount ? 0 : next;
                        })}>Next Page
                        </button>
                    </div>
                </div>
            </div>
        );
    }

}