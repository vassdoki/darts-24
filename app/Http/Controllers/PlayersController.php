<?php
/**
 * @author David Namenyi <dnamenyi@gmail.com>
 * Date: 2016.08.05.
 */

namespace App\Http\Controllers;

use App\Models\Game;
use App\Models\Player;
use Illuminate\Http\Request;

class PlayersController extends Controller {

    /**
     * Returns a list of players for a specified game session with their scores.
     *
     * @param Request $request
     * @return array
     */
    public function index(Request $request)
    {
        $this->validate($request, [
            'game_id' => 'int'
        ]);

        $gameId = $request->input('game_id');

        if (empty($gameId))
            return Player::all();
        else
            return Player::whereGameId($gameId)->get();

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
    }

    /**
     * Returns the details of a given player.
     *
     * @param int $id
     * @return array
     */
    public function show($id)
    {
        return Player::whereId($id)->with('game')->firstOrFail();
        return [
            'id' => $id,
            'name' => 'Peter Griffin',
            'created_at' => '2016-08-04 12:44:12',
            'updated_at' => '2016-08-04 13:29:50',
            'scores' => [
                'STRUCTURE NOT YET DEFINED'
            ],
        ];
    }

    /**
     * Creates a new player for a specified game session.
     *
     * @param Request $request
     * @return array
     */
    public function create(Request $request)
    {
        $this->validate($request, [
            'game_id' => 'required|int',
            'player_name' => 'required|string'
        ]);

        $game = Game::whereId($request->input('game_id'))->firstOrFail();

        $player = Player::create([
            'game_id' => $game->id,
            'name' => $request->input('player_name')
        ]);

        return [
            'player_id' => $player->id
        ];
    }

}