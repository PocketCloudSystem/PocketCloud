<?php

namespace pocketcloud\config;

use pocketcloud\utils\Config;
use pocketcloud\utils\SingletonTrait;

class SignLayoutConfig {
    use SingletonTrait;

    public const DEFAULT_LAYOUTS = [
        0 => [ //lobby
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§aLOBBY§8]", "§eO§7oooo"],
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§aLOBBY§8]", "§7o§eO§7ooo"],
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§aLOBBY§8]", "§7oo§eO§7oo"],
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§aLOBBY§8]", "§7ooo§eO§7o"],
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§aLOBBY§8]", "§7oooo§eO§7"]
        ],
        1 => [ //full
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§cFULL§8]", "§eO§7oooo"],
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§cFULL§8]", "§7o§eO§7ooo"],
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§cFULL§8]", "§7oo§eO§7oo"],
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§cFULL§8]", "§7ooo§eO§7o"],
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§cFULL§8]", "§7oooo§eO§7"]
        ],
        2 => [ //search
            ["§l%template%", "§cSearching for", "§cfree server...", "§eO§7oooo"],
            ["§l%template%", "§cSearching for", "§cfree server...", "§7o§eO§7ooo"],
            ["§l%template%", "§cSearching for", "§cfree server...", "§7oo§eO§7oo"],
            ["§l%template%", "§cSearching for", "§cfree server...", "§7ooo§eO§7o"],
            ["§l%template%", "§cSearching for", "§cfree server...", "§7oooo§eO§7"]
        ],
        3 => [ //maintenance
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§bMAINTENANCE§8]", "§eO§7oooo"],
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§bMAINTENANCE§8]", "§7o§eO§7ooo"],
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§bMAINTENANCE§8]", "§7oo§eO§7oo"],
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§bMAINTENANCE§8]", "§7ooo§eO§7o"],
            ["§l%server%", "§e%players%§8/§e%max_players%", "§8[§bMAINTENANCE§8]", "§7oooo§eO§7"]
        ]
    ];

    private Config $config;

    public function __construct() {
        self::setInstance($this);
        $this->config = new Config(IN_GAME_PATH . "signLayouts.yml", 2, self::DEFAULT_LAYOUTS);
    }

    public function reload(): void {
        $this->config->reload();
    }

    public function getConfig(): Config {
        return $this->config;
    }
}