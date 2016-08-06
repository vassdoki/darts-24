<?php
/**
 * @author David Namenyi <dnamenyi@gmail.com>
 * Date: 2016.08.06.
 */

namespace App\Http\Controllers;

use App\Models\GameType;
use Illuminate\Http\Request;

class GameTypesController extends Controller {

    /**
     * Returns a list of the available game types.
     *
     * @return array
     */
    public function index()
    {
        return GameType::all();
    }

    /**
     * Returns the details of a game type
     *
     * @param int $id
     * @return array
     */
    public function show($id)
    {
        return GameType::whereId($id)->firstOrFail();
    }

    /**
     * Creates a new game type.
     *
     * @param Request $request
     * @return array
     */
    public function create(Request $request)
    {
        $this->validate($request, [
            'name' => 'required|string',
            'description' => 'required|string',
            'config' => 'string'
        ]);

        $gameType = GameType::create([
            'name' => $request->input('name'),
            'description' => $request->input('description'),
            'config' => $request->input('config')
        ]);

        return [
            'game_type_id' => $gameType->id
        ];
    }

}