<?php

namespace pocketcloud\cloud\thread;

use pmmp\thread\Worker as NativeWorker;

abstract class Worker extends NativeWorker {
    use ThreadPartsTrait;
}