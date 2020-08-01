# scrabble-api

POST: /generate
```json
{
    "board": [{"row": 7, "tiles": "-------basic---"}],  
    "rack": "*nfjobi"  
}
``` 
```json
{
    "count": 2153,
    "candidates": [  
      ...
    ],
    "context": {
        "board": [
            {
                "row": 7,
                "contents": "-------basic---"
            }
        ],
        "rack": "*nfjobi",
        "raw": false
    }
}
```
