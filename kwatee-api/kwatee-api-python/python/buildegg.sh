#!/bin/sh
rm -rf *.egg ../../dist/addons/python/egg/* ../../dist/addons/python/doc/*
python setup.py bdist_egg --dist-dir=.
rm -rf build kwatee.egg-info
egg=`ls *.egg`
mv $egg ../../dist/addons/python/egg/ ; echo "Moved $egg to:\n   `cd ../../dist/addons/python/egg && pwd`"
echo "Generating doc in:\n   `cd ../../dist/addons/python/doc && pwd`"
epydoc --config kwatee.epydoc.config