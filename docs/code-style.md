Code Style
==========

Unless otherwise stated, try to follow the [Effective Scala][1] Guide.

 * `Command.apply()` should be defined _and called_ with parentheses. Your `applyInternal`
    may not have side effects, but a command in general does.

[1]: http://twitter.github.io/effectivescala/