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


$app->get('/', function () use ($app) {
    return 'darts-24 back-end API';
});


$app->get('/game-types', function() {
    return \App\Models\GameType::all();
});


$app->post('/games', 'GamesController@create');
$app->get('/games', 'GamesController@index');
$app->get('/games/{id}', 'GamesController@show');


$app->post('/players', 'PlayersController@create');
$app->get('/players', 'PlayersController@index');
$app->get('/players/{id}', 'PlayersController@show');


$app->post('/scores', 'ScoresController@create');