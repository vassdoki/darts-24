<?php
/**
 * @author David Namenyi <dnamenyi@gmail.com>
 * Date: 2016.08.05.
 */

namespace App\Http\Controllers;

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
            'game_id' => 'required|int'
        ]);

        // $request->input('game_id');

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

        // $request->input('game_id');
        // $request->input('player_name');

        return [
            'player_id' => 123
        ];
    }

}