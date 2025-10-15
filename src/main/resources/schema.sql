CREATE TABLE IF NOT EXISTS arenas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    world TEXT NOT NULL,
    spawn1_x REAL NOT NULL,
    spawn1_y REAL NOT NULL,
    spawn1_z REAL NOT NULL,
    spawn1_yaw REAL DEFAULT 0,
    spawn1_pitch REAL DEFAULT 0,
    spawn2_x REAL NOT NULL,
    spawn2_y REAL NOT NULL,
    spawn2_z REAL NOT NULL,
    spawn2_yaw REAL DEFAULT 0,
    spawn2_pitch REAL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS match_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    arena_id INTEGER NOT NULL,
    player1_uuid TEXT NOT NULL,
    player1_name TEXT NOT NULL,
    player2_uuid TEXT NOT NULL,
    player2_name TEXT NOT NULL,
    winner_uuid TEXT,
    result_type TEXT NOT NULL,
    duration_seconds INTEGER NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (arena_id) REFERENCES arenas(id)
);

CREATE TABLE IF NOT EXISTS player_stats (
    player_uuid TEXT PRIMARY KEY,
    player_name TEXT NOT NULL,
    total_matches INTEGER DEFAULT 0,
    wins INTEGER DEFAULT 0,
    losses INTEGER DEFAULT 0,
    draws INTEGER DEFAULT 0,
    kills INTEGER DEFAULT 0,
    deaths INTEGER DEFAULT 0,
    total_playtime_seconds INTEGER DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);