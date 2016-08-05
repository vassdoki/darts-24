<?php
/**
 * @author David Namenyi <dnamenyi@gmail.com>
 * Date: 2016.08.05.
 */

namespace App\Http\Controllers;

use App\Models\Game;
use App\Models\GameType;
use Illuminate\Http\Request;

class GamesController extends Controller {

    /**
     * Returns a list of the currently open game sessions with their details.
     *
     * @return array
     */
    public function index()
    {
        return Game::with('gameType')->get();
    }

    /**
     * Returns the details of a given game session.
     *
     * @param int $id
     * @return array
     */
    public function show($id)
    {
        return Game::whereId($id)->with('gameType')->firstOrFail();
    }

    /**
     * Starts a new dart game session with the specified game type.
     *
     * @param Request $request
     * @return array
     */
    public function create(Request $request)
    {
        $this->validate($request, [
            'game_type_id' => 'required|int'
        ]);

        $gameType = GameType::whereId($request->input('game_type_id'))->firstOrFail();

        $game = Game::create([
            'game_type_id' => $gameType->id
        ]);

        return [
            'game_id' => $game->id
        ];
    }

}