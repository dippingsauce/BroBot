#!/bin/sh
files="$HOME/BroBot/tits.txt"
grep -v '^$' $files > $files.out
mv $files.out $files
