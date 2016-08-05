<?php
/**
 * @author David Namenyi <dnamenyi@gmail.com>
 * Date: 2016.08.05.
 */

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Player extends Model {

    protected $table = 'players';

    protected $fillable = ['name', 'game_id'];

    /**
     * Get the Game that the player belongs to.
     */
    public function game()
    {
        return $this->belongsTo('App\Models\Game');
    }

    /**
     * Get the scores of the player
     */
    public function scores()
    {
        return $this->hasMany('App\Models\Score');
    }

}