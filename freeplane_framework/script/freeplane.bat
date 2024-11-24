java ^
--add-exports=java.desktop/sun.awt.shell=ALL-UNNAMED ^
--add-exports=java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED ^
-XX:+IgnoreUnrecognizedVMOptions ^
-XX:-ProfileExceptionHandlers ^
-XX:MaxRAM=20g -XX:MaxRAMPercentage=15.0 ^
-Djava.security.manager=allow ^
-Dorg.freeplane.userfpdir="%APPDATA%\\Freeplane" ^
-jar freeplanelauncher.jar %*
