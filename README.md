![Moon Dweller](/resources/public/images/heading.png?raw=true "Moon Dweller")

MOON DWELLER: A text-based adventure in your web browser.

**You've woken abruptly in a small, silver-walled room with no windows. There is a door to the east. What will you do? The choice is yours...**

[PLAY IT NOW AT moondweller.io](http://moondweller.io/).

Type `help` in the game if you get stuck!

## History

This is a re-imagining of [Medieval Alien Massacre](https://github.com/buntine/Medieval-Alien-Massacre/), a text-based game I wrote back in 2010 in Clojure. I've ported it to ClojureScript here so it can run in the browser.

## Running locally

    $ lein cljsbuild once
    $ lein ring server-headless

## Notes

 - Type the `help` command in gameplay for some tips and instructions.
 - You can `save` and `load` at any time. It uses localStorage and supports only one save slot.
 - Inspired by Dunnet, by Rob Schnell (`$ emacs -batch -l dunnet`).

This project is dedicated to the memory of my friend, Adam Hillier.

## License

Copyright © 2015 Andrew Buntine (http://bunts.io)

Distributed under the GNU GPL. See the LICENSE file.
