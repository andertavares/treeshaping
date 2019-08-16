ant

jar cvfm treeshaping.jar build/treeshaping.mf -C ./bin .

echo "Finished. Run with java -cp .:lib/*:treeshaping.jar [mainclass] -c configfile [other-parameters]"