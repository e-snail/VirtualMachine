#!/bin/sh

rm -rf androidvm 

cp ../app/build/intermediates/transforms/dex/debug/folders/1000/1f/main/classes.dex androidvm_classes.dex 

java -jar baksmali-2.2.0.jar d androidvm_classes.dex -o androidvm 
