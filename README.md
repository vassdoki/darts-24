# darts-24 back-end

PHP API built on Laravel

Uses JSON objects and HTTP response codes for communication

On success, it returns 200 HTTP response code and the response in JSON
On failure, it returns non-200 HTTP response codes, e.g.:

+ 404 (Not Found)
+ 422 (Unprocessable Entity) - most probably means a validation error
+ 500 (Internal Server Error)

- - -

## API documentation

#### GET /game-types

Returns a list of the available game types:

```
[
    {
        id: 123,
        name: '301',
        description: 'The simple 301 dart game',
        created_at: '2016-08-05 15:48:06',
        updated_at: '2016-08-05 15:48:06',
        config: { CUSTOM RULES IN JSON }
    },
    ...
]
```

#### POST /games

Starts a new dart game session with the specified game type.

Required parameters: game_type_id (int)

On success, returns the id of the new game session.

```
{
    game_id: 123
}
```

#### GET /games

Returns a list of the currently open game sessions with their details.

```
[
    {
        id: 123,
        game_type_id: 123,
        game_type: {
            GAMETYPE OBJECT DEFINED ABOVE
        },
        created_at: '2016-08-04 12:44:12',
        updated_at: '2016-08-04 13:29:50',
        closed: false
    },
    ...
]
```

#### GET /games/{ID}

Returns the details of a given game session.

```
{
    id: 123,
    game_type_id: 123,
    game_type: {
        GAMETYPE OBJECT DEFINED ABOVE
    },
    created_at: '2016-08-04 12:44:12',
    updated_at: '2016-08-04 13:29:50',
    closed: false
}
```

#### POST /games/{ID}/close

Closes a dart game session

```
{
    success: true
}
```

#### POST /players

Creates a new player for a specified game session.

Required parameters: game_id (int), player_name (string)

On success, returns the id of the new player.

```
{
    player_id: 123
}
```

#### GET /players

Returns a list of players for a specified game session with their scores.

Optional parameters: game_id (int)

```
[
    {
        id: 123,
        name: 'John Stephenson',
        game_id: 123,
        created_at: '2016-08-04 12:44:12',
        updated_at: '2016-08-04 13:29:50',
        scores: [
            {
                id: 123
                game_id: 234
                player_id: 345
                score: '20'
                modifier: 'd' // Signals double (d) and triple (t) shots
                round_hash: '546715f5c6ce9e9b695593d8c53a90f8' // Consecutive shots from the same player have the same hash -- can be used to determine which shots belong to the same round
                created_at: '2016-08-05 21:57:24'
                updated_at: '2016-08-05 21:57:24'
            },
            ...
        ]
    },
    ...
]
```

#### GET /players/{ID}

Returns the details of a given player.

```
{
    id: 123,
    name: 'John Stephenson',
    game_id: 123,
    created_at: '2016-08-04 12:44:12',
    updated_at: '2016-08-04 13:29:50',
    scores: [
        SCORE OBJECT DEFINED ABOVE,
        ...
    ]
}
```

#### POST /scores

Saves a new score

Required parameters: game_id (int), player_id (int), score (int)

Optional parameters: modifier (string, 1 character long)

If saving is successful, returns this:
```
{
    success: true
}
```