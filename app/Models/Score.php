<?php
/**
 * @author David Namenyi <dnamenyi@gmail.com>
 * Date: 2016.08.05.
 */

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Score extends Model {

    protected $table = 'scores';

    protected $fillable = ['player_id', 'game_id', 'score', 'round_hash'];

    /**
     * Get the Player that the score belongs to.
     * @return \App\Models\Player
     */
    public function player()
    {
        return $this->belongsTo('App\Models\Player');
    }

    /**
     * Get the Game that the score belongs to.
     * @return \App\Models\Game
     */
    public function game()
    {
        return $this->belongsTo('App\Models\Game');
    }

}