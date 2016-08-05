<?php
/**
 * @author David Namenyi <dnamenyi@gmail.com>
 * Date: 2016.08.05.
 */

namespace App\Http\Controllers;

use Illuminate\Http\Request;

class GamesController extends Controller {

    /**
     * Returns a list of the currently open game sessions with their details.
     *
     * @return array
     */
    public function index()
    {
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
    }

    /**
     * Returns the details of a given game session.
     *
     * @param int $id
     * @return array
     */
    public function show($id)
    {
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

        // $request->input('game_type_id');

        return [
            'game_id' => 123
        ];
    }

}