<?php
/**
 * @author David Namenyi <dnamenyi@gmail.com>
 * Date: 2016.08.05.
 */

namespace App\Http\Controllers;

use Illuminate\Http\Request;

class ScoresController extends Controller {

    /**
     * Saves a new score
     *
     * @param Request $request
     * @return array
     */
    public function create(Request $request)
    {
        $this->validate($request, [
            'game_id' => 'required|int',
            'player_id' => 'required|int',
            'score' => 'required|int'
        ]);

        // $request->input('game_id');
        // $request->input('player_id');
        // $request->input('score');

        return [
            'success' => true
        ];
    }

}