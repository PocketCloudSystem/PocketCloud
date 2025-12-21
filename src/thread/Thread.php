<?php

namespace pocketcloud\cloud\thread;

use pmmp\thread\Thread as NativeThread;

abstract class Thread extends NativeThread {
    use ThreadPartsTrait;
}