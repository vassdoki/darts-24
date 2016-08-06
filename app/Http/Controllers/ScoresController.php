<?php
/**
 * @author David Namenyi <dnamenyi@gmail.com>
 * Date: 2016.08.05.
 */

namespace App\Http\Controllers;

use App\Models\Game;
use App\Models\Player;
use App\Models\Score;
use Carbon\Carbon;
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
            'score' => 'required|int',
            'modifier' => 'string|size:1'
        ]);

        $game = Game::whereId($request->input('game_id'))->firstOrFail();
        $player = Player::whereId($request->input('player_id'))->firstOrFail();

        $lastScore = Score::whereGameId($game->id)->orderBy('id', 'desc')->take(1)->get();

        $roundHash =
            (!empty($lastScore[0]) AND ($lastScore[0] instanceof Score) AND $lastScore[0]->player->id === $player->id) ?
            $lastScore[0]->round_hash :
            hash('md5', Carbon::now()->toDateTimeString());

        $score = Score::create([
            'game_id' => $game->id,
            'player_id' => $player->id,
            'score' => $request->input('score'),
            'round_hash' => $roundHash,
            'modifier' => $request->input('modifier')
        ]);

        $scriptPath = base_path() . '/camera/shoot.sh';
        $photosPath = base_path() . '/camera/photos';
        shell_exec("sh $scriptPath $photosPath {$score->id}");

        return [
            'success' => true
        ];
    }

}