# scrabble-api

POST: /generate  

*Note that a \* in the request context rack denotes a blank tile.
Furthermore, a capital letter in the request context board represents
a blank whose the effective letter is given by the lowercase version of the given value.*

```json
{
    "board": [{"row": 7, "tiles": "-------Basic---"}],  
    "rack": "*nfjobi"  
}
``` 

```json
{
    "count": 2153,
    "candidates": [  
      "..."
    ],
    "context": {
        "board": [
            {
                "row": 7,
                "tiles": "-------Basic---"
            }
        ],
        "rack": "*nfjobi",
        "raw": false
    }
}
```
