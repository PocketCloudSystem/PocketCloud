<?php

namespace pocketcloud\cloud\http\util;

use pocketcloud\cloud\util\trait\EnumHelperTrait;

enum RequestMethod {
    use EnumHelperTrait;

    case GET;
    case POST;
    case PATCH;
    case DELETE;
    case PUT;
}