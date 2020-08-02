async function executeQuery(query) {
    const response = await fetch("/generate", {
        method: "POST",
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(query)
    });
    return response.json();
}

const tiles = [];
for (let i = 0; i < 15; i++) {
    tiles.push([]);
}

function cell(c) {
    const cell = document.createElement("div");
    cell.className = "flex centering cell";
    const input = document.createElement("input");
    if (c !== '_') {
        input.value = c;
    }
    input.maxLength = 1;
    cell.append(input);
    return cell;
}

function buildBoard(result) {
    document.body.className = "flex centering";
    const div = document.createElement("div");
    div.className = "flex col";
    let i = 0;
    for (const row of result.serializedBoard) {
        const rowDiv = document.createElement("div");
        !i++ && rowDiv.append(cell('_'));
        rowDiv.className = "flex";
        div.append(rowDiv);
        for (const c of row) {
            c !== ' ' && rowDiv.append(cell(c));
        }
    }
    document.body.append(div);
}

function buildCandidateList(result) {
    const listDiv = document.createElement("div");
    listDiv.className = "flex col scroll";
    listDiv.style.width = "50%";
    listDiv.style.height = "50%";
    listDiv.style.marginLeft = "50px";
    for (const candidate of result.candidates) {
        const span = document.createElement("span");
        span.textContent = candidate;
        span.style.marginBottom = "10px";
        listDiv.append(span);
    }
    document.body.append(listDiv);
}

async function main() {
    const result = await executeQuery({
        board: [
            {row: 7, tiles: "-------basic---"},
            {row: 8, tiles: "---------a-----"},
            {row: 9, tiles: "---------d-----"}
        ],
        rack: "*nfjobi"
    });
    buildBoard(result);
    buildCandidateList(result);
    return result;
}

main().then(console.log);