#!/bin/bash

# When started, watches the whole source directory for changes. When a change happens it
# starts the unit tests, and then goes back to waiting.

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=`dirname "$SCRIPT"`

WATCHFILE=/tmp/src-test-watch-output.txt
ANT="ant -logger org.apache.tools.ant.listener.AnsiColorLogger"

rm $WATCHFILE > /dev/null

which inotifywait > /dev/null
if [[ $? == 0 ]]; then
  echo "Monitoring src folder for changes"
  pushd ${SCRIPTPATH}/../..
  # Rather than recursively set up watches for the whole source dir every time,
  # we instead set them up once to report to a file in the background, then we
  # can just watch for changes to that log file.
  inotifywait --excludei "(open|access|close)" -r -m src > $WATCHFILE &
  while inotifywait $WATCHFILE; do
    nice $ANT test
  done
else
  echo "The inotifywait command is not available. It is available on Ubuntu from the inotify-tools package."
fi
