<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\CommandArgument;
use pocketcloud\cloud\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\web\WebAccount;
use pocketcloud\cloud\web\WebAccountManager;

final readonly class WebAccountArgument extends CommandArgument {

    public function parseValue(string $input): WebAccount {
        if (($account = WebAccountManager::getInstance()->get($input)) !== null) return $account;
        throw new ArgumentParseException();
    }

    public function getType(): string {
        return "web_account";
    }
}