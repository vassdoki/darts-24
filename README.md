# darts-24 back-end

PHP API built on Laravel

Uses JSON objects and HTTP response codes for communication

+ on success, it returns 200 HTTP response code and the response in JSON
+ on known exception, it returns 200 HTTP response code and the error message in JSON

```
{
    error: true,
    message: 'Invalid game_id'
}
```
+ on unknown exception, it returns 500 HTTP response code

## API documentation

#### GET /game-types

Returns a list of the available game types:
```
{
    {
        id: 123,
        name: '301',
        description: 'The simple 301 dart game'
    },
    ...
}
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
{
    {
        id: 123,
        game_type: {
            id: 123,
            name: '301',
            description: 'The simple 301 dart game'
        },
        created_at: '2016-08-04 12:44:12'
        updated_at: '2016-08-04 13:29:50',
    },
    ...
}
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

#### POST /players

Create a new player for a specified game session.

Parameters: game_id, player_name

On success, returns the id of the new player.

```
{
    player_id: 123
}
```

#### GET /players

Parameters: game_id

Returns a list of players for a specified game session with their scores.
```
{
    {
        id: 123,
        name: 'John Stephenson',
        scores: {
            STRUCTURE NOT YET DEFINED
        }
    },
    ...
}
```
