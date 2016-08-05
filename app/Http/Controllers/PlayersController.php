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
            return Player::with('scores')->get();
        else
            return Player::whereGameId($gameId)->with('scores')->get();
    }

    /**
     * Returns the details of a given player.
     *
     * @param int $id
     * @return array
     */
    public function show($id)
    {
        return Player::whereId($id)->with('game', 'scores')->firstOrFail();
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