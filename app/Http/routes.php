<?php

/*
|--------------------------------------------------------------------------
| Application Routes
|--------------------------------------------------------------------------
|
| Here is where you can register all of the routes for an application.
| It is a breeze. Simply tell Lumen the URIs it should respond to
| and give it the Closure to call when that URI is requested.
|
*/

use Illuminate\Http\Request;

function returnError($message) {
    return [
        'error' => true,
        'message' => $message
    ];
}

$app->get('/', function () use ($app) {
    return 'darts-24 back-end API';
});

$app->get('/game-types', function() {
    return [
        [
            'id' => 123,
            'name' => '301',
            'description' => 'The simple 301 dart game'
        ]
    ];
});

$app->post('/games', function(Request $request) {
    $game_type_id = $request->input('game_type_id');

    if (empty($game_type_id) OR !is_numeric($game_type_id))
        return returnError('Invalid or missing game_type_id');

    return [
        'game_id' => 123
    ];
});

$app->get('/games', function() {
    return [
        [
            'id' => 123,
            'game_type' => [
                'id' => 123,
                'name' => '301',
                'description' => 'The simple 301 dart game'
            ],
            'created_at' => '2016-08-03 09:44:12',
            'updated_at' => '2016-08-03 10:29:50',
        ],
        [
            'id' => 234,
            'game_type' => [
                'id' => 123,
                'name' => '301',
                'description' => 'The simple 301 dart game'
            ],
            'created_at' => '2016-08-04 12:44:12',
            'updated_at' => '2016-08-04 13:29:50',
        ]
    ];
});

$app->get('/games/{id}', function($id) {
    return [
        'id' => $id,
        'game_type' => [
            'id' => 123,
            'name' => '301',
            'description' => 'The simple 301 dart game'
        ],
        'created_at' => '2016-08-04 12:44:12',
        'updated_at' => '2016-08-04 13:29:50',
    ];
});

$app->post('/players', function(Request $request) {
    $game_id = $request->input('game_id');

    if (empty($game_id) OR !is_numeric($game_id))
        return returnError('Invalid or missing game_id');

    $player_name = $request->input('player_name');

    if (empty($player_name) OR strlen($player_name) < 1)
        return returnError('Invalid or missing player_name');

    return [
        'player_id' => 123
    ];
});

$app->get('/players', function() {
    return [
        [
            'id' => 123,
            'name' => 'John Stephenson',
            'created_at' => '2016-08-03 09:44:12',
            'updated_at' => '2016-08-04 10:29:50',
            'scores' => [
                'STRUCTURE NOT YET DEFINED'
            ],
        ],
        [
            'id' => 234,
            'name' => 'Peter Griffin',
            'created_at' => '2016-08-04 12:44:12',
            'updated_at' => '2016-08-04 13:29:50',
            'scores' => [
                'STRUCTURE NOT YET DEFINED'
            ],
        ]
    ];
});

$app->post('/scores', function(Request $request) {
    $game_id = $request->input('game_id');

    if (empty($game_id) OR !is_numeric($game_id))
        return returnError('Invalid or missing game_id');

    $player_id = $request->input('player_id');

    if (empty($player_id) OR strlen($player_id) < 1)
        return returnError('Invalid or missing player_id');

    $score = $request->input('score');

    if (empty($score) OR !is_numeric($score))
        return returnError('Invalid or missing score');

    return [
        'success' => true
    ];
});