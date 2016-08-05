<?php

use App\Models\GameType;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    /**
     * Run the database seeds.
     *
     * @return void
     */
    public function run()
    {
        // $this->call('UserTableSeeder');

        $gameTypes = [
            [
                'name' => '301',
                'description' => 'The simple 301 dart game'
            ]
        ];

        foreach ($gameTypes as $gameType) {
            $model = GameType::where('name', $gameType['name'])->first();
            if ( $model instanceof GameType ) continue;
            GameType::create($gameType);
        }
    }
}
