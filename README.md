# PocketCloud (BETA)

## Requirements
- Linux OS

## Installation
- Create a folder for the cloud
- Download this [PHP-Binary](https://jenkins.pmmp.io/job/PHP-8.0-Aggregate/)
- Put the "bin" directory in the folder
- Download the ".phar" File from the releases
- Put the .phar file in the folder
- Make a start file with this content: `bin/php7/bin/php PocketCloud.phar`
- Give the start file permissions and execute the script

## Features
- Templates
- Dynamic Servers
- Customizable Messages (Location: storage/inGame/messages.json)
- Maintenance System
- Automatically downloading server software (PocketMine, WaterdogPE) & plugins (CloudBridge)
- Cloud Plugins (Location: storage/plugins/cloud)
- Modules (can be activated/deactivated in the modules.json located in storage/inGame)
- Custom sign layouts (also located in storage/inGame)
- Notify (In-game notifications)

## NOTE
- You don't have to download the plugins (CloudBridge, CloudBridge-Proxy)
- If you want your server to save when the server stops, then you need to turn on "auto-save" in server.properties

## Modules
- **Sign Module** (Enabled by default)
- **NPC Module** (Enabled by default)
- **HubCommand Module** (Enabled by default)
- **GlobalChat Module** (Disabled by default)

## Commands | NOTE: **BOLD** Arguments = Required (Cloud)
| Usage                                            | Description                                       | Area        |
|--------------------------------------------------|---------------------------------------------------|-------------|
| exit                                             | Stops the cloud & the running servers             | General     |
| help                                             | Get a list of all commands                        | General     |
| list                                             | Get a list of all servers, players & templates    | General     |
| create **[name]** [type (server, proxy): server] | Create a template                                 | Template    |
| delete **[template]**                            | Delete a template                                 | Template    |
| edit **[template]** **[key]** **[value]**        | Create a template                                 | Template    |
| maintenance **[add, remove, list]** [player]     | Add/Remove a player to/from the maintenance list  | Maintenance |
| start **[template]** [count: 1]                  | Start a server                                    | Server      |
| stop **[template, server, all]**                 | Stop a server, template or all servers            | Server      |
| save **[server]**                                | Copy the server files to the template             | Server      |
| execute **[server]** **[commandLine]**           | Send a command to a server                        | Server      |
| enable **[plugin]**                              | Enable a plugin                                   | Plugin      |
| disable **[plugin]**                             | Disable a plugin                                  | Plugin      |
| plugins                                          | Get a list of all plugins                         | Plugin      |
| kick **[player]** [reason]                       | Kick a player                                     | Player      |

## Commands | NOTE: **BOLD** Arguments = Required (CloudBridge)
| Usage                                | Description                             | Permission                   |
|--------------------------------------|-----------------------------------------|------------------------------|
| /cloud **[start, stop, save, list]** | Manage the servers in-game              | pocketcloud.command.cloud    |
| /cloudnotify                         | Activate/Deactivate your notifications  | pocketcloud.command.notify   |
| /transfer **[server]** [player]      | Transfer a player to a server           | pocketcloud.command.transfer |

## Commands (NPC Module)
| Usage                           | Description      | Permission                   |
|---------------------------------|------------------|------------------------------|
| /cloudnpc                       | Manage the NPCs  | pocketcloud.command.cloudnpc |

## Commands (HubCommand Module)
| Usage | Description         | Permission |
|-------|---------------------|------------|
| /hub  | Transfer to a lobby | NONE       |

## Sign Module
- **How to create a sign?** Very easy just place a sign with the first line ist [PocketCloud] and the second line is your template (for example Lobby).
- **How to remove a sign?** You have to destroy the sign.
- **How can I transfer to another server?** Just interact with the sign.

## NPC Module
- **How to create a npc?** Very easy just type "/cloudnpc" in the chat, click on "Create a NPC" and specify your details.
- **How to remove a npc?** You have to type "/cloudnpc" in the chat, click on "Remove a NPC" and left-click on your npc entity.
- **How can I transfer to another server?** You can left-click on the npc to see all servers with the template provided at the creation, but you also can right-click on the npc to use the quick join.

## Permissions
| Name                           | Description                          | Command/Action           |
|--------------------------------|--------------------------------------|--------------------------|
| pocketcloud.command.cloud      | You can use the /cloud command       | /cloud                   |
| pocketcloud.command.notify     | You can use the /cloudnotify command | /cloudnotify             |
| pocketcloud.command.transfer   | You can use the /transfer command    | /transfer                |
| pocketcloud.command.cloudnpc   | You can use the /cloudnpc command    | /cloudnpc                |
| pocketcloud.cloudsign.add      | You can create a cloudsign           | Create a CloudSign       |
| pocketcloud.cloudsign.remove   | You can remove a cloudsign           | Remove a CloudSign       |
| pocketcloud.notify.receive     | You can receive notifications        | Receive notifications    |
| pocketcloud.maintenance.bypass | You can bypass the maintenance mode  | Bypass maintenance mode  |

## BETA
- If you find bugs or something like that please create an [issue](https://github.com/PocketCloudSystem/PocketCloud/issues/new?assignees=&labels=&template=bug_report.md&title=) it would be very kind! :heart:

## Config
```json
{
  "cloud-port": 5678,
  "debug-mode": false
}
```

## Modules 
```json
{
    "sign-module": true,
    "npc-module": true,
    "global-chat-module": false,
    "hubcommand-module": true
}
```

## Messages
```json
{
  "server-start": "{PREFIX}§7The server §e%server% §7is §astarting§7...",
  "server-could-not-start": "{PREFIX}§7The server §e%server% §ccouldn't §7be started§7!",
  "server-timed-out": "{PREFIX}§7The server §e%server% §7is §ctimed out§7!",
  "server-stop": "{PREFIX}§7The server §e%server% §7is §cstopping§7...",
  "server-crashed": "{PREFIX}§7The server §e%server% §7was §ccrashed§7!",
  "proxy-stopped": "§f§lProxy was stopped!",
  "cloud-command-description": "Cloud Command",
  "cloud-notify-command-description": "CloudNotify Command",
  "transfer-command-description": "Transfer Command",
  "hub-command-description": "Hub Command",
  "cloud-npc-command-description": "CloudNPC Command",
  "no-permissions": "{PREFIX}§7You don't have the permissions to use this command!",
  "request-timeout": "§cRequest §e%0% §8(§e%1%§8) §ctimed out",
  "notifications-activated": "{PREFIX}§7You are now getting notifications!",
  "notifications-deactivated": "{PREFIX}§7You are now no longer getting notifications!",
  "cloud-help-usage": "{PREFIX}§c/cloud start <template> [count: 1]\n{PREFIX}§c/cloud stop <template|server>\n{PREFIX}§c/cloud save\n{PREFIX}§c/cloud list [type (servers|templates|players): servers]",
  "cloud-list-help-usage": "{PREFIX}§c/cloud list [type (servers|templates|players): servers]",
  "cloud-start-help-usage": "{PREFIX}§c/cloud start <template> [count: 1]",
  "cloud-stop-help-usage": "{PREFIX}§c/cloud stop <template|server>",
  "server-existence": "{PREFIX}§cThe server doesn't exists!",
  "template-existence": "{PREFIX}§cThe template doesn't exists!",
  "max-servers": "{PREFIX}§7The maximum server amount for the template has been reached!",
  "server-saved": "{PREFIX}§7The server was saved!",
  "template-maintenance": "§cThis template is in maintenance!",
  "connect-to-server": "{PREFIX}§7Connecting to server §e%0%§7...",
  "already-connected": "{PREFIX}§7You are already connected to the server §e%0%§7",
  "cant-connect": "{PREFIX}§7Can't connect to the server §e%0%§7!",
  "npc-name-tag": "§7%0% playing.\n§8× §e§l%1%",
  "process-cancelled": "{PREFIX}§7The process has been cancelled!",
  "select-npc": "{PREFIX}§7Please select a npc to remove by hitting on them!",
  "already-npc": "{PREFIX}§7There is already a cloud npc!",
  "npc-created": "{PREFIX}§7The npc was successfully created!",
  "npc-removed": "{PREFIX}§7The NPC has been successfully removed!",
  "no-server-found": "{PREFIX}§7No server found!",
  "already-in-lobby": "{PREFIX}§7You are already in a lobby!",
  "transfer-help-usage": "{PREFIX}§c/transfer <server> [player]",
  "ui-npc-choose-server-title": "§8» §eList §r§8| §e%0% §8«",
  "ui-npc-choose-server-text": "§7There are currently §e%0% servers §7with the template §e%1% §7available.",
  "ui-npc-choose-server-button": "§e%0%\n§8» §a%1%§8/§c%2%",
  "ui-npc-choose-server-no-server": "§cNo server available!"
}
```

## SignLayout
```yaml
- - - §l§b%server% #Online-State Layout
    - §e%players%§8/§e%max_players%
    - §8[§aLOBBY§8]
    - §eO§7oooo
  - - §l§b%server%
    - §e%players%§8/§e%max_players%
    - §8[§aLOBBY§8]
    - §7o§eO§7ooo
  - - §l§b%server%
    - §e%players%§8/§e%max_players%
    - §8[§aLOBBY§8]
    - §7oo§eO§7oo
  - - §l§b%server%
    - §e%players%§8/§e%max_players%
    - §8[§aLOBBY§8]
    - §7ooo§eO§7o
  - - §l§b%server%
    - §e%players%§8/§e%max_players%
    - §8[§aLOBBY§8]
    - §7oooo§eO§7
- - - §l§b%server% #Full-State Layout
    - §e%players%§8/§e%max_players%
    - §8[§cFULL§8]
    - §eO§7oooo
  - - §l§b%server%
    - §e%players%§8/§e%max_players%
    - §8[§cFULL§8]
    - §7o§eO§7ooo
  - - §l§b%server%
    - §e%players%§8/§e%max_players%
    - §8[§cFULL§8]
    - §7oo§eO§7oo
  - - §l§b%server%
    - §e%players%§8/§e%max_players%
    - §8[§cFULL§8]
    - §7ooo§eO§7o
  - - §l§b%server%
    - §e%players%§8/§e%max_players%
    - §8[§cFULL§8]
    - §7oooo§eO§7
- - - §l%template% #No Free Server Layout
    - §cSearching for
    - §cfree server...
    - §eO§7oooo
  - - §l%template%
    - §cSearching for
    - §cfree server...
    - §7o§eO§7ooo
  - - §l%template%
    - §cSearching for
    - §cfree server...
    - §7oo§eO§7oo
  - - §l%template%
    - §cSearching for
    - §cfree server...
    - §7ooo§eO§7o
  - - §l%template%
    - §cSearching for
    - §cfree server...
    - §7oooo§eO§7
- - - §l§b%server% #Maintenance Layout
    - §e%players%§8/§e%max_players%
    - §8[§bMAINTENANCE§8]
    - §eO§7oooo
  - - §l§b%server%
    - §e%players%§8/§e%max_players%
    - §8[§bMAINTENANCE§8]
    - §7o§eO§7ooo
  - - §l§b%server%
    - §e%players%§8/§e%max_players%
    - §8[§bMAINTENANCE§8]
    - §7oo§eO§7oo
  - - §l§b%server%
    - §e%players%§8/§e%max_players%
    - §8[§bMAINTENANCE§8]
    - §7ooo§eO§7o
  - - §l§b%server%
    - §e%players%§8/§e%max_players%
    - §8[§bMAINTENANCE§8]
    - §7oooo§eO§7
```