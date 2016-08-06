<?php
/**
 * @author David Namenyi <dnamenyi@gmail.com>
 * Date: 2016.08.05.
 */

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class GameType extends Model {

    protected $table = 'game_types';

    protected $fillable = ['name', 'description', 'config'];

}