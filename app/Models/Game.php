<?php
/**
 * @author David Namenyi <dnamenyi@gmail.com>
 * Date: 2016.08.05.
 */

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Game extends Model {

    protected $table = 'games';

    protected $fillable = ['game_type_id'];

    /**
     * Get the GameType that the game session belongs to.
     */
    public function gameType()
    {
        return $this->belongsTo('App\Models\GameType');
    }

    /**
     * Get the players for the game session
     */
    public function players()
    {
        return $this->hasMany('App\Models\Player');
    }

}