# darts-24 back-end

PHP API built on Laravel

Uses JSON objects and HTTP response codes for communication

On success, it returns 200 HTTP response code and the response in JSON
On failure, it returns non-200 HTTP response codes, e.g.:

+ 404 (Not Found)
+ 422 (Unprocessable Entity) - most probably means a validation error
+ 500 (Internal Server Error)

## API documentation

#### GET /game-types

Returns a list of the available game types:
```
[
    {
        id: 123,
        name: '301',
        description: 'The simple 301 dart game'
    },
    ...
]
```

#### POST /games

Starts a new dart game session with the specified game type.

Parameters: game_type_id

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
        game_type: {
            id: 123,
            name: '301',
            description: 'The simple 301 dart game'
        },
        created_at: '2016-08-04 12:44:12',
        updated_at: '2016-08-04 13:29:50',
    },
    ...
]
```

#### GET /games/{ID}

Returns the details of a given game session.
```
{
    id: 123,
    game_type: {
        id: 123,
        name: '301',
        description: 'The simple 301 dart game'
    },
    created_at: '2016-08-04 12:44:12',
    updated_at: '2016-08-04 13:29:50'
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

Parameters: game_id, player_name

On success, returns the id of the new player.

```
{
    player_id: 123
}
```

#### GET /players

Returns a list of players for a specified game session with their scores.

Parameters: game_id
```
[
    {
        id: 123,
        name: 'John Stephenson',
        scores: {
            STRUCTURE NOT YET DEFINED
        }
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
    scores: {
        STRUCTURE NOT YET DEFINED
    }
}
```

#### POST /scores

Saves a new score

Parameters: game_id, player_id, score

If saving is successful, returns this:
```
{
    success: true
}
```