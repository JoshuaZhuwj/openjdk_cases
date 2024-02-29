java -Xbatch --add-modules jdk.incubator.vector -XX:CompileCommand=print,*.test_* -XX:LoopUnrollLimit=0 -XX:+UseZGC -XX:+ZGenerational Test 2>&1 | tee output.log
